(ns prone.ui
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :refer [put! chan <!]]
            [cljs.reader :as reader]
            [prone.map-browser :refer [MapBrowser]]
            [prone.uitilities :refer [action]]
            [quiescent :as q :include-macros true]
            [quiescent.dom :as d]))

(defn update-in* [m path f]
  "Like update-in, but can map over lists by nesting paths."
  (if (vector? (last path))
    (let [nested-path (last path)
          this-path (drop-last path)]
      (if (empty? nested-path)
        (update-in m this-path (partial map f))
        (update-in m this-path (partial map #(update-in* % nested-path f)))))
    (update-in m path f)))

(defn filter-frames [frame-filter frames]
  (case frame-filter
    :all frames
    :application (filter :application? frames)))

(def source-classes {:clj "language-clojure"
                     :java "language-java"})

(q/defcomponent StackFrame [frame select-frame]
  (d/li {:className (when (:selected? frame) "selected")
         :onClick (action #(put! select-frame (:id frame)))}
        (d/span {:className "stroke"}
                (d/span {:className (if (:application? frame)
                                      "icon application"
                                      "icon")})
                (d/div {:className "info"}
                       (if (= (:lang frame) :clj)
                         (d/div {:className "name"}
                                (d/strong {} (:package frame))
                                (d/span {:className "method"} "/" (:method-name frame)))
                         (d/div {:className "name"}
                                (d/strong {} (:package frame) "." (:class-name frame))
                                (d/span {:className "method"} "$" (:method-name frame))))
                       (if (:file-name frame)
                         (d/div {:className "location"}
                                (:loaded-from frame) " "
                                (d/span {:className "filename"}
                                        (:file-name frame))
                                ", line "
                                (d/span {:className "line"} (:line-number frame)))
                         (d/div {:className "location"}
                                "(unknown file)"))))))

(q/defcomponent StackInfo [frame]
  (d/header {:className "trace_info clearfix"}
            (d/div {:className "title"}
                   (d/h2 {:className "name"} (:method-name frame))
                   (d/div {:className "location"}
                          (d/span {:className "filename"}
                                  (:class-path-url frame))))
            (if-let [source-code (-> frame :source :code)]
              (d/div {:className "code_block clearfix"}
                     (d/pre {:className "line-numbers code"
                             :data-line (:line-number frame)
                             :data-line-offset (:offset (:source frame))}
                            (d/code {:className (source-classes (:lang frame))} source-code)))
              (d/div {:className "source-failure"}
                     (-> frame :source :failure)))))

(q/defcomponent ProneUI
  "Prone's main UI component - the page's frame"
  [{:keys [error request frame-filter paths]} chans]
  (let [error (get-in error (:error paths))]
    (d/div {:className "top"}
           (d/header {:className "exception"}
                     (d/h2 {}
                           (d/strong {} (:type error))
                           (d/span {} " at " (:uri request))
                           (when (or (:caused-by error) (seq (:error paths)))
                             (d/span {:className "caused-by"}
                                     (when (seq (:error paths))
                                       (d/a {:href "#"
                                             :onClick (action #(put! (:navigate-error chans) [:reset (drop 1 (:error paths))]))}
                                            "< back"))
                                     (when-let [caused-by (:caused-by error)]
                                       (d/span {} " Caused by " (d/a {:href "#"
                                                                      :onClick (action #(put! (:navigate-error chans) [:concat [:caused-by]]))}
                                                                     (:type caused-by)))))))
                     (d/p {} (or (:message error)
                                 (d/span {} (:class-name error)
                                         (d/span {:className "subtle"} " [no message]")))))
           (d/section {:className "backtrace"}
                      (d/nav {:className "sidebar"}
                             (d/nav {:className "tabs"}
                                    (d/a {:href "#"
                                          :className (when (= :application frame-filter) "selected")
                                          :onClick (action #(put! (:change-frame-filter chans) :application))}
                                         "Application Frames")
                                    (d/a {:href "#"
                                          :className (when (= :all frame-filter) "selected")
                                          :onClick (action #(put! (:change-frame-filter chans) :all))}
                                         "All Frames"))
                             (apply d/ul {:className "frames" :id "frames"}
                                    (map #(StackFrame % (:select-frame chans))
                                         (filter-frames frame-filter (:frames error)))))
                      (d/div {:className "frame_info" :id "frame-info"}
                             (StackInfo (first (filter :selected? (:frames error))))
                             (when (:data error)
                               (d/div {:className "sub"}
                                      (MapBrowser {:data (:data error)
                                                   :path (:data paths)
                                                   :name "Exception data"} (:navigate-data chans))))
                             (d/div {:className "sub"}
                                    (MapBrowser {:data request
                                                 :path (:request paths)
                                                 :name "Request map"} (:navigate-request chans))))))))

(defn update-selected-frame [data frame-id]
  (let [path (concat [:error] (-> data :paths :error) [:frames []])]
    (update-in* data path
                #(if (= (:id %) frame-id)
                   (assoc % :selected? true)
                   (dissoc % :selected?)))))

(defn navigate-map [name data navigation]
  (case (first navigation)
    :concat (update-in data [:paths name] #(concat % (second navigation)))
    :reset (assoc-in data [:paths name] (second navigation))))

(defn code-excerpt-changed? [old new]
  (not (and (= (:error new) (:error old))
            (= (-> new :paths :error) (-> old :paths :error)))))

(let [chans {:select-frame (chan)
             :change-frame-filter (chan)
             :navigate-request (chan)
             :navigate-data (chan)
             :navigate-error (chan)}
      prone-data (atom nil)]
  (go-loop []
    (when-let [frame-id (<! (:select-frame chans))]
      (swap! prone-data update-selected-frame frame-id)
      (recur)))

  (go-loop []
    (when-let [filter (<! (:change-frame-filter chans))]
      (swap! prone-data assoc :frame-filter filter)
      (recur)))

  (go-loop []
    (when-let [navigation (<! (:navigate-request chans))]
      (swap! prone-data (partial navigate-map :request) navigation)
      (recur)))

  (go-loop []
    (when-let [navigation (<! (:navigate-data chans))]
      (swap! prone-data (partial navigate-map :data) navigation)
      (recur)))

  (go-loop []
    (when-let [navigation (<! (:navigate-error chans))]
      (swap! prone-data (partial navigate-map :error) navigation)
      (recur)))

  (add-watch
   prone-data
   :state-change
   (fn [key ref old new]
     (q/render (ProneUI new chans)
               (.getElementById js/document "ui-root"))
     (when (code-excerpt-changed? old new)
       (.highlightAll js/Prism)
       (set! (-> js/document (.getElementById "frame-info") .-scrollTop) 0))))

  (let [data-text (-> js/document (.getElementById "prone-data") .-innerHTML)
        data (reader/read-string data-text)]
    (reset! prone-data data)))

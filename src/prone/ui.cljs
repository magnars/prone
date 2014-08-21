(ns prone.ui
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :refer [put! chan <!]]
            [cljs.reader :as reader]
            [quiescent :as q :include-macros true]
            [quiescent.dom :as d]))

(defn action [f]
  (fn [e]
    (.preventDefault e)
    (f)))

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
                                (d/span {:className "filename"}
                                        (:file-name frame))
                                ", line "
                                (d/span {:className "line"} (:line-number frame)))
                         (d/div {:className "location"}
                                "(unknown file)"))))))

(def source-classes {:clj "language-clojure"
                     :java "language-java"})

(q/defcomponent StackInfo [frame]

  (d/div {:className "frame_info"}
         (d/header {:className "trace_info clearfix"}
                   (d/div {:className "title"}
                          (d/h2 {:className "name"} (:method-name frame))
                          (d/div {:className "location"}
                                 (d/span {:className "filename"}
                                         (:class-path-url frame))))
                   (d/div {:className "code_block clearfix"}
                          (d/pre {}
                                 (d/code {:className (source-classes (:lang frame))}
                                         (:source frame)))))))

(q/defcomponent ProneUI
  "Prone's main UI component - the page's frame"
  [{:keys [error request frame-filter]} chans]
  (d/div {:className "top"}
         (d/header {:className "exception"}
                   (d/h2 {}
                         (d/strong {} (:type error))
                         (d/span {} " at " (:uri request)))
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
                    (StackInfo (first (filter :selected? (:frames error)))))))

(defn update-selected-frame [data frame-id]
  (update-in* data [:error :frames []] #(if (= (:id %) frame-id)
                                          (assoc % :selected? true)
                                          (dissoc % :selected?))))

(let [chans {:select-frame (chan)
             :change-frame-filter (chan)}
      prone-data (atom nil)]
  (go-loop []
           (when-let [frame-id (<! (:select-frame chans))]
             (swap! prone-data update-selected-frame frame-id)
             (recur)))

  (go-loop []
           (when-let [filter (<! (:change-frame-filter chans))]
             (swap! prone-data assoc :frame-filter filter)
             (recur)))

  (add-watch
   prone-data
   :state-change
   (fn [key ref old new]
     (q/render (ProneUI new chans)
               (.getElementById js/document "ui-root"))))

  (let [data-text (-> js/document (.getElementById "prone-data") .-innerHTML)
        data (reader/read-string data-text)]
    (reset! prone-data data)))

(ns prone.ui.components.app
  (:require [cljs.core.async :refer [put! map>]]
            [prone.ui.components.map-browser :refer [MapBrowser InlineVectorBrowser ValueToken]]
            [prone.ui.components.source-location :refer [SourceLocation]]
            [prone.ui.components.code-excerpt :refer [CodeExcerpt]]
            [prone.ui.utils :refer [action]]
            [quiescent.core :as q :include-macros true]
            [quiescent.dom :as d]))

(q/defcomponent ErrorHeader
  [{:keys [error location paths]} chans]
  (d/header {:className "exception"}
            (d/h2 {}
              (d/strong {} (:type error))
              (d/span {} " at " location)
              (when (or (:caused-by error)
                        (seq (:error paths))
                        (:other-error paths))
                (d/span {:className "caused-by"}
                  (if (seq (:other-error paths))
                    (d/a {:href "#"
                          :onClick (action #(put! (:navigate-data chans)
                                                  [:other-error [:reset nil]]))}
                      "< back")
                    (when (seq (:error paths))
                      (d/a {:href "#"
                            :onClick (action #(put! (:navigate-data chans)
                                                    [:error [:reset (butlast (:error paths))]]))}
                        "< back")))
                  (when-let [caused-by (:caused-by error)]
                    (d/span {} " Caused by " (d/a {:href "#"
                                                   :onClick (action #(put! (:navigate-data chans)
                                                                           [:error [:concat [:caused-by]]]))}
                                               (:type caused-by)))))))
            (d/p {} (or (:message error)
                        (d/span {} (:class-name error)
                                (d/span {:className "subtle"} " [no message]"))))))

(q/defcomponent DebugHeader []
  (d/header {:className "exception"}
            (d/h2 {} (d/span {} "Tired of seeing this page? Remove calls to prone.debug/debug"))
            (d/p {} "Halted for debugging")))

(q/defcomponent Header
  [data chans]
  (if (:error data)
    (ErrorHeader data chans)
    (DebugHeader)))

(q/defcomponent SourceLocLink
  [src-loc-selection target name chans]
  (d/a {:href "#"
        :className (when (= target src-loc-selection) "selected")
        :onClick (action #(put! (:change-src-loc-selection chans) target))}
       name))

(q/defcomponent Sidebar
  [{:keys [error src-loc-selection selected-src-loc debug-data active-src-locs]} chans]
  (d/nav {:className "sidebar"}
         (d/nav {:className "tabs"}
                (when error
                  (SourceLocLink src-loc-selection :application "Application Frames" chans))
                (when error
                  (SourceLocLink src-loc-selection :all "All Frames" chans))
                (when (seq debug-data)
                  (SourceLocLink src-loc-selection :debug "Debug Calls" chans)))
         (apply d/ul {:className "frames" :id "frames"}
                (map #(SourceLocation {:src-loc %, :selected? (= % selected-src-loc)}
                                  (:select-src-loc chans))
                     active-src-locs))))

(q/defcomponent ExceptionsWhenRealizing [exceptions-when-realizing navigate-data]
  (d/div {:className "sub"}
    (d/h3 {:className "map-path"} "Exceptions while realizing request map")
    (d/div {:className "inset variables"}
      (d/table  {:className "var_table"}
        (apply d/tbody {}
               (for [[path exception] exceptions-when-realizing]
                 (d/tr {}
                   (d/td {:className "name"} (InlineVectorBrowser path nil))
                   (d/td {} (d/a {:href "#"
                                  :onClick (action #(put! navigate-data [:other-error [:reset [:exceptions-when-realizing path]]]))}
                              (or (:message exception)
                                  (:class-name exception)))))))))))

(q/defcomponent Body
  [{:keys [src-loc-selection selected-src-loc debug-data error paths browsables exceptions-when-realizing] :as data} {:keys [navigate-data]}]
  (let [debugging? (= :debug src-loc-selection)
        local-browsables (:browsables (if debugging? selected-src-loc error))
        heading (when (= :debug src-loc-selection) (:message debug-data))]
    (apply d/div {:className "frame_info" :id "frame-info"}
           (CodeExcerpt selected-src-loc)
           (when heading (d/h2 {:className "sub-heading"} heading))
           (when (seq exceptions-when-realizing)
             (ExceptionsWhenRealizing exceptions-when-realizing navigate-data))
           (map #(d/div {:className "sub"}
                   (MapBrowser {:data (:data %)
                                :path (get paths %)
                                :name (:name %)}
                               (map> (fn [v] [% v]) navigate-data)))
                (concat local-browsables browsables)))))

(q/defcomponent ProneUI
  "Prone's main UI component - the page's frame"
  [data chans]
  (d/div {:className "top"}
         (Header data chans)
         (d/section {:className "backtrace"}
                    (Sidebar data chans)
                    (Body data chans))))

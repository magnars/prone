(ns prone.ui.components.app
  (:require [cljs.core.async :refer [put!]]
            [prone.ui.components.map-browser :refer [MapBrowser]]
            [prone.ui.components.stack-frame :refer [StackFrame]]
            [prone.ui.components.code-excerpt :refer [CodeExcerpt]]
            [prone.ui.utils :refer [action]]
            [quiescent :as q :include-macros true]
            [quiescent.dom :as d]))

(defn filter-frames [frame-filter frames]
  (case frame-filter
    :all frames
    :application (filter :application? frames)))

(q/defcomponent ErrorHeader
  [{:keys [error request paths]} chans]
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
                                (d/span {:className "subtle"} " [no message]"))))))

(q/defcomponent DebugHeader
  [{:keys [request]} chans]
  (d/header {:className "exception"}
            (d/h2 {}
                  (d/span {} "Tired of seeing this page? Remove calls to "
                          "prone.debug/debug - and stop causing exceptions"))
            (d/p {} "Halted for debugging")))

(q/defcomponent Header
  [data chans]
  (if (:error data)
    (ErrorHeader data chans)
    (DebugHeader data chans)))

(q/defcomponent StackFrameLink
  [{:keys [frame-filter target name]} chans]
  (d/a {:href "#"
        :className (when (= target frame-filter) "selected")
        :onClick (action #(put! (:change-frame-filter chans) target))}
       name))

(q/defcomponent Sidebar
  [{:keys [error frame-filter debug-data]} chans]
  (d/nav {:className "sidebar"}
         (apply d/nav {:className "tabs"}
                (when error
                  (StackFrameLink {:frame-filter frame-filter
                                   :target :application
                                   :name "Application Frames"} chans))
                (when error
                  (StackFrameLink {:frame-filter frame-filter
                                   :target :all
                                   :name "All Frames"} chans))
                (when (seq debug-data)
                  (StackFrameLink {:frame-filter frame-filter
                                   :target :debug
                                   :name "Debug Calls"} chans)))

         (apply d/ul {:className "frames" :id "frames"}
                (cond
                 (and error (#{:application :all} frame-filter))
                 (map #(StackFrame % (:select-frame chans))
                      (filter-frames frame-filter (:frames error)))

                 (= :debug frame-filter)
                 (map #(StackFrame % (:select-frame chans))
                      debug-data)))))

(q/defcomponent Body
  [{:keys [error request paths]} chans]
  (d/div {:className "frame_info" :id "frame-info"}
         (CodeExcerpt (first (filter :selected? (:frames error))))
         (when (:data error)
           (d/div {:className "sub"}
                  (MapBrowser {:data (:data error)
                               :path (:data paths)
                               :name "Exception data"} (:navigate-data chans))))
         (d/div {:className "sub"}
                (MapBrowser {:data request
                             :path (:request paths)
                             :name "Request map"} (:navigate-request chans)))))

(q/defcomponent ProneUI
  "Prone's main UI component - the page's frame"
  [data chans]
  (d/div {:className "top"}
         (Header data chans)
         (d/section {:className "backtrace"}
                    (Sidebar data chans)
                    (Body data chans)
                    )))

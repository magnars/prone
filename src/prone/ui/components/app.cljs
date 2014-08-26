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

(q/defcomponent Header
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

(q/defcomponent Sidebar
  [{:keys [error frame-filter]} chans]
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
                     (filter-frames frame-filter (:frames error))))))

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
                    (Body data chans))))

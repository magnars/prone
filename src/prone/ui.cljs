(ns prone.ui
  (:require [cljs.reader :as reader]
            [quiescent :as q :include-macros true]
            [quiescent.dom :as d]))

(q/defcomponent StackFrame [frame]
  (d/li {:className "frame"}
        (d/span {:className "stroke"}
                (d/span {:className "icon"})
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

(q/defcomponent ErrorDetails
  "Prone's main UI component - the page's frame"
  [{:keys [error request]}]
  (d/div {:className "top"}
         (d/header {:className "exception"}
                   (d/h2 {}
                         (d/strong {} (:type error))
                         (d/span {} " at " (:uri request)))
                   (d/p {} (:message error)))
         (d/section {:className "backtrace"}
                    (d/nav {:className "sidebar"}
                           (d/nav {:className "tabs"}
                                  (d/a {:href "#"} "Application Frames")
                                  (d/a {:href "#" :className "selected"} "All Frames"))
                           (apply d/ul {:className "frames" :id "frames"}
                                  (map StackFrame (:frames error))))
                    (StackInfo (first (:frames error))))))

(def prone-data (atom nil))

(add-watch
 prone-data
 :state-change
 (fn [key ref old new]
   (q/render (ErrorDetails new)
             (.getElementById js/document "ui-root"))))

(let [data-text (-> js/document (.getElementById "prone-data") .-innerHTML)]
  (reset! prone-data (reader/read-string data-text)))

(ns prone.ui.components.code-excerpt
  (:require [quiescent :as q :include-macros true]
            [quiescent.dom :as d]))

(def source-classes {:clj "language-clojure"
                     :java "language-java"})

(q/defcomponent CodeExcerpt [frame]
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

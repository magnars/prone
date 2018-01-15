(ns prone.ui.components.code-excerpt
  (:require [quiescent.core :as q :include-macros true]
            [quiescent.dom :as d]))

(def source-classes {:clj "language-clojure"
                     :java "language-java"})

(q/defcomponent CodeExcerpt [source-loc]
  (d/header {:className "trace_info clearfix"}
            (d/div {:className "title"}
                   (d/h2 {:className "name"} (:method-name source-loc))
                   (d/div {:className "location"}
                          (d/span {:className "filename"}
                                  (:class-path-url source-loc))))
            (if-let [source-code (-> source-loc :source :code)]
              (d/div {:className "code_block clearfix"}
                     (d/pre {:className "line-numbers code"
                             :data-line (:line-number source-loc)
                             :data-line-offset (-> source-loc :source :offset)}
                            (d/code {:className (source-classes (:lang source-loc))} source-code)))
              (d/div {:className "source-failure"}
                     (-> source-loc :source :failure)))))

(ns prone.ui.components.source-location
  (:require [cljs.core.async :refer [put!]]
            [prone.ui.utils :refer [action]]
            [quiescent.core :as q :include-macros true]
            [quiescent.dom :as d]))

(q/defcomponent SourceLocation [{:keys [src-loc selected?]} select-src-loc]
  (d/li {:className (when selected? "selected")
         :onClick (action #(put! select-src-loc src-loc))}
        (d/span {:className "stroke"}
                (d/span {:className (if (:application? src-loc)
                                      "icon application"
                                      "icon")})
                (d/span {:className "info"}
                       (if (= (:lang src-loc) :clj)
                         (d/span {:className "name"}
                                (d/strong {} (:package src-loc))
                                (d/span {:className "method"} "/" (:method-name src-loc)))
                         (d/span {:className "name"}
                                (d/strong {} (:package src-loc) "." (:class-name src-loc))
                                (d/span {:className "method"} "$" (:method-name src-loc))))
                       (if (:file-name src-loc)
                         (d/div {:className "location"}
                                (:loaded-from src-loc) " "
                                (d/span {:className "filename"}
                                        (:file-name src-loc))
                                ", line "
                                (d/span {:className "line"} (:line-number src-loc)))
                         (d/div {:className "location"}
                                "(unknown file)"))))))

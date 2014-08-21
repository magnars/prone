(ns prone.rendering
  "Functions to render exception and request/response data as HTML"
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [prone.hiccough :refer :all]
            [prone.prep :refer [prep]]))

(defn with-layout [title & markup-snippets]
  (list "<!DOCTYPE html>"
        [:html
         [:head
          [:title title]
          [:style (slurp (io/resource "prone/better-errors.css"))]
          [:style (slurp (io/resource "prone/prism.css"))]
          [:style (slurp (io/resource "prone/styles.css"))]]
         [:body
          markup-snippets
          [:script (slurp (io/resource "prone/react-0.11.1.js"))]
          [:script (slurp (io/resource "prone/generated/prone.js"))]
          [:script (slurp (io/resource "prone/prism.js"))]
          [:script (slurp (io/resource "prone/prism.clojure.js"))]]]))

(defn render-exception [request {:keys [message] :as error}]
  (render
   (with-layout message
     [:div {:id "ui-root"}]
     [:script {:type "text/json"
               :id "prone-data"}
      (prn-str (prep error request))])))

(ns prone.core
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [prone.debug :as debug]
            [prone.hiccough :refer [render]]
            [prone.prep :refer [prep-error-page prep-debug-page]]
            [prone.stacks :refer [normalize-exception]]))

(defn- serve [html & [status]]
  {:status (or status 500)
   :body html
   :headers {"Content-Type" "text/html"}})

(defn get-application-name []
  (second (edn/read-string (slurp "project.clj"))))

(defn render-page [data]
  (render
   (list "<!DOCTYPE html>"
         [:html
          [:head
           [:title (:title data)]
           [:style (slurp (io/resource "prone/better-errors.css"))]
           [:style (slurp (io/resource "prone/prism.css"))]
           [:style (slurp (io/resource "prone/styles.css"))]]
          [:body
           [:div {:id "ui-root"}]
           [:script {:type "text/json" :id "prone-data"} (prn-str data)]
           [:script (slurp (io/resource "prone/react-0.11.1.js"))]
           [:script (slurp (io/resource "prone/prism.js"))]
           [:script (slurp (io/resource "prone/prism-line-numbers.js"))]
           [:script (slurp (io/resource "prone/prism.clojure.js"))]
           [:script (slurp (io/resource "prone/generated/prone.js"))]]])))

(defn wrap-exceptions [handler]
  (fn [req]
    (reset! debug/debug-data [])
    (try
      (let [result (handler req)]
        (if (< 0 (count @debug/debug-data))
          (-> @debug/debug-data
              (prep-debug-page req)
              render-page
              (serve 203))
          result))
      (catch Exception e
        (.printStackTrace e)
        (-> e
            normalize-exception
            (prep-error-page @debug/debug-data req (get-application-name))
            render-page
            serve)))))

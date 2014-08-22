(ns prone.core
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [prone.hiccough :refer [render]]
            [prone.prep :refer [prep]]
            [prone.stacks :refer [normalize-exception]]))

(defn- serve [html]
  {:status 200
   :body html
   :headers {"Content-Type" "text/html"}})

(defn get-application-name []
  (second (edn/read-string (slurp "project.clj"))))

(defn render-page [data]
  (render
   (list "<!DOCTYPE html>"
         [:html
          [:head
           [:title (-> data :error :message)]
           [:style (slurp (io/resource "prone/better-errors.css"))]
           [:style (slurp (io/resource "prone/prism.css"))]
           [:style (slurp (io/resource "prone/styles.css"))]]
          [:body
           [:div {:id "ui-root"}]
           [:script {:type "text/json" :id "prone-data"} (prn-str data)]
           [:script (slurp (io/resource "prone/react-0.11.1.js"))]
           [:script (slurp (io/resource "prone/prism.js"))]
           [:script (slurp (io/resource "prone/prism.clojure.js"))]
           [:script (slurp (io/resource "prone/generated/prone.js"))]]])))

(defn wrap-exceptions [handler]
  (fn [req]
    (try
      (handler req)
      (catch Exception e
        (-> e
            normalize-exception
            (prep req (get-application-name))
            render-page
            serve)))))

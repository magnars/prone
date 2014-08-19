(ns prone.core
  (:require [prone.rendering :refer [render-exception]]
            [prone.stacks :refer [normalize-exception]]))

(defn- serve [html]
  {:status 200
   :body html
   :headers {"Content-Type" "text/html"}})

(defn wrap-exceptions [handler]
  (fn [req]
    (try
      (handler req)
      (catch Exception e
        (->> e normalize-exception (render-exception req) serve)))))

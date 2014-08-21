(ns prone.demo
  (:require [clojure.java.io :as io]
            [prone.core :refer [wrap-exceptions]]))

(defn handler [req]
  (if (re-find #"prone.js.map$" (:uri req))
    {:status 200
     :headers {"Content-Type" "application/octet-stream"}
     :body (slurp (io/resource "prone/generated/prone.js.map"))}
    (throw (Exception. "Oh noes!"))))

(def app
  (-> handler
      wrap-exceptions))

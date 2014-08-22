(ns prone.demo
  (:require [clojure.java.io :as io]
            [prone.core :refer [wrap-exceptions]]))

(defn piggie-backer [app]
  (fn [req]
    (app (assoc req :nested {:maps {:are {:certainly {:supported 42}}}}))))

(defn handler [req]
  (cond

   ;; serve source maps
   (re-find #"prone.js.map$" (:uri req))
   {:status 200
    :headers {"Content-Type" "application/octet-stream"}
    :body (slurp (io/resource "prone/generated/prone.js.map"))}

   ;; throw exception in dependency (outside of app)
   (= (:uri req) "/external-throw") (re-find #"." nil)

   ;; basic case
   :else (throw (Exception. "Oh noes!"))))

(def app
  (-> handler
      wrap-exceptions
      piggie-backer))

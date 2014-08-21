(ns prone.prep
  (:require [clojure.java.io :as io]))

(defn- load-source [frame]
  (assoc frame :source (if-not (:class-path-url frame)
                         "(unknown source file)"
                         (if-not (io/resource (:class-path-url frame))
                           "(could not locate source file on class path)"
                           (slurp (io/resource (:class-path-url frame)))))))

(defn prep [error request]
  {:error (-> error
              (update-in [:frames]
                         #(->> %
                               (map-indexed (fn [idx f] (assoc f :id idx)))
                               (mapv load-source)))
              (update-in [:frames 0] assoc :selected? true))
   :request {:uri (:uri request)}})

(ns prone.demo
  (:require [prone.core :refer [wrap-exceptions]]))

(defn handler [req]
  (throw (Exception. "Oh noes!")))

(def app
  (-> handler
      wrap-exceptions))

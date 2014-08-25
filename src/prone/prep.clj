(ns prone.prep
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.walk :as walk]
            [prone.clj-code-trunc :as clj-code])
  (:import [java.io InputStream]))

(defn- load-source [frame]
  (assoc frame :source (if-not (:class-path-url frame)
                         {:code "(unknown source file)" :start-line 1}
                         (if-not (io/resource (:class-path-url frame))
                           {:code "(could not locate source file on class path)" :start-line 1}
                           (clj-code/truncate (slurp (io/resource (:class-path-url frame)))
                                              (:line-number frame)
                                              500)))))

(defn- set-application-frame [application-name frame]
  (if (and (:package frame)
           (.startsWith (:package frame) (str application-name ".")))
    (assoc frame :application? true)
    frame))

(defn select-starting-frame [frames]
  (if-let [first-frame (or (first (filter :application? frames))
                           (first frames))]
    (update-in frames [(:id first-frame)] assoc :selected? true)
    frames))

(defn- get-type [val]
  (-> val
      type
      str
      (str/replace #"^class " "")))

(defn- prepare-for-serialization-1 [val]
  (cond
   (nil? val) val
   (map? val) val
   (vector? val) val
   (list? val) val
   (set? val) val
   (seq? val) val
   (string? val) val
   (number? val) val
   (keyword? val) val
   (= true val) val
   (= false val) val
   (instance? InputStream val) {::to-string (slurp val)
                                ::original-type (get-type val)}
   :else {::to-string (.toString val)
          ::original-type (get-type val)}))

(defn- prepare-for-serialization [m]
  (walk/postwalk prepare-for-serialization-1 m))

(defn- prep-error [error application-name]
  (-> (if (:caused-by error)
        (update-in error [:caused-by] #(prep-error % application-name))
        error)
      (update-in [:frames]
                 #(->> %
                       (map-indexed (fn [idx f] (assoc f :id idx)))
                       (map (partial set-application-frame application-name))
                       (mapv load-source)))
      (update-in [:frames] select-starting-frame)))

(defn prep [error request application-name]
  {:error (prep-error error application-name)
   :request (prepare-for-serialization request)
   :frame-filter :application
   :paths {:request []
           :data []
           :error []}})

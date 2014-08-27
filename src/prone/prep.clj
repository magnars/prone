(ns prone.prep
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.walk :as walk]
            [prone.code-trunc :as clj-code])
  (:import [java.io InputStream File]))

(defonce max-source-lines 500)

(defn- load-source [{:keys [class-path-url line-number] :as src-loc}]
  (if-not class-path-url
    {:failure "(unknown source file)"}
    (if-let [source (io/resource class-path-url)]
      (clj-code/truncate (slurp source) line-number max-source-lines)
      {:failure "(could not locate source file on class path)"})))

(defn- add-source [src-loc]
  (assoc src-loc :source (load-source src-loc)))

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

(defn- to-string
  "Create a string representation of a class, prefering ones that does not include
   type information - since we already display that next to it."
  [val]
  (let [s (pr-str val)]
    (if (.startsWith s "#<")
      (.toString val)
      s)))

(defn- prepare-for-serialization-1 [val]
  (cond
   (nil? val) val
   (instance? java.lang.Class val) {::value (symbol (.getName val))
                                    ::original-type "java.lang.Class"}
   (instance? clojure.lang.IRecord val) {::value (into {} val)
                                         ::original-type (.getName (type val))}
   (instance? InputStream val) {::value (slurp val)
                                ::original-type (get-type val)}
   (map? val) val
   (vector? val) val
   (list? val) val
   (set? val) val
   (seq? val) val
   (string? val) val
   (number? val) val
   (keyword? val) val
   (symbol? val) val
   (= true val) val
   (= false val) val
   :else {::value (to-string val)
          ::original-type (get-type val)}))

(defn- prepare-for-serialization [m]
  (walk/prewalk prepare-for-serialization-1 m))

(defn- prep-error [error application-name]
  (-> (if (:caused-by error)
        (update-in error [:caused-by] #(prep-error % application-name))
        error)
      (update-in [:frames]
                 #(->> %
                       (map-indexed (fn [idx f] (assoc f :id idx)))
                       (map (partial set-application-frame application-name))
                       (mapv add-source)))
      (update-in [:frames] select-starting-frame)
      (update-in [:data] prepare-for-serialization)))

(defn- prep-debug-1 [{:keys [class-path-url] :as debug}]
  (let [root-dir (str/replace (.getAbsolutePath (File. ".")) #"\.$" "")
        resource (and class-path-url (io/resource class-path-url))
        file-name (and resource (.getPath resource))]
    (merge debug {:lang :clj
                  :file-name (when file-name (str/replace file-name root-dir ""))
                  :method-name "[unknown]"
                  :package (and class-path-url (-> class-path-url
                                                   (str/replace #"\.[^/]*$" "")
                                                   (str/replace "/" ".")
                                                   (str/replace "_" "-")))
                  :source (load-source debug)})))

(defn- prep-debug [debug-data]
  (-> (mapv prep-debug-1 debug-data)
      prepare-for-serialization
      (update-in [0] assoc :selected? true)))

(defn prep-error-page [error debug-data request application-name]
  (let [prepped-error (prep-error error application-name)]
    {:title (-> prepped-error :message)
     :error prepped-error
     :debug-data (prep-debug debug-data)
     :request (prepare-for-serialization request)
     :frame-filter :application
     :paths {:request []
             :data []
             :error []}}))

(defn prep-debug-page [debug-data request]
  {:title "Debug halt"
   :request (prepare-for-serialization request)
   :debug-data (prep-debug debug-data)
   :frame-filter :debug})

(ns prone.prep
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.walk :as walk]
            [prone.code-trunc :as clj-code])
  (:import [java.io InputStream File]))

(defonce max-source-lines 500)

(defn- load-source-file [source line-number]
  (if (nil? source)
    {:failure "(could not locate source file on class path)"}
    (let [file (if (instance? java.net.URL source) (File. (.getPath source)) source)]
      (if-not (.exists file)
        {:failure "(could not locate source file)"}
        (clj-code/truncate (slurp file) line-number max-source-lines)))))

(defn- load-source [frame]
  (assoc frame :source (if-not (:class-path-url frame)
                         {:failure "(unknown source file)"}
                         (load-source-file (io/resource (:class-path-url frame))
                                           (:line-number frame)))))

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
   (symbol? val) val
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

(defn- prep-debug-1 [{:keys [file-name line-number] :as debug}]
  (let [root-dir (str/replace (.getAbsolutePath (File. ".")) #"\.$" "")]
    (merge debug {:lang :clj
                  :file-name (str/replace file-name root-dir "")
                  :method-name "[unknown]"
                  :package (-> file-name
                               (str/replace #"^.*(src|test)/" "")
                               (str/replace #"\.[^/]*$" "")
                               (str/replace "/" ".")
                               (str/replace "_" "-"))
                  :source (load-source-file (File. file-name) line-number)})))

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

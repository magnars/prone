(ns prone.prep
  "Prepare data for serialization so it can safely go over the wire, and be
  picked back up with read-string on the client."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.walk :as walk]
            [prone.code-trunc :as clj-code])
  (:import [java.io InputStream File IOException]))

(def max-source-lines
  "Source code files longer than this will need to be truncated in order for the
  client-side syntax highlighter to finish in a timely fashion."
  500)

(def max-string-length
  "While it's nice to have all the data available in the UI, some strings are
  just too long for performance and display purposes. Truncate them down."
  10000)

(defn- load-source
  "Attempt to load source code from the classpath"
  [{:keys [class-path-url line-number] :as src-loc}]
  (if-not class-path-url
    {:failure "(unknown source file)"}
    (if-let [source (io/resource class-path-url)]
      (clj-code/truncate (slurp source) line-number max-source-lines)
      {:failure "(could not locate source file on class path)"})))

(defn- add-source [src-loc]
  (assoc src-loc :source (load-source src-loc)))

(defn- set-application-frame
  "If the namespace of an exception frame starts with the application name,
  consider the frame to be an application frame."
  [app-namespaces frame]
  (if (and (:package frame)
           (some #(.startsWith (:package frame) (str % ".")) app-namespaces))
    (assoc frame :application? true)
    frame))

(defn- get-type [val]
  (-> val
      type
      str
      (str/replace #"^class " "")))

(defn- to-string
  "Create a string representation of a class, preferring ones that does not
  include type information - since we already display that next to it."
  [val]
  (let [s (pr-str val)]
    (if (or (.startsWith s "#<")
            (.startsWith s "#object["))
      (.toString val)
      s)))

(defn- prepare-for-serialization-1 [val]
  (cond
    (nil? val) val
    (instance? java.lang.Class val) {::value (symbol (.getName val))
                                     ::original-type "java.lang.Class"}
    (instance? clojure.lang.IRecord val) {::value (into {} val)
                                          ::original-type (.getName (type val))}
    (instance? InputStream val) {::value (try
                                           (slurp val)
                                           (catch IOException _ nil))
                                 ::original-type (get-type val)}
    (string? val) (let [len (count val)]
                    (if (< max-string-length len)
                      {::value (str (subs val 0 60) "...")
                       ::original-type (str "String with " len " chars")}
                      val))
    (map? val) val
    (vector? val) val
    (list? val) val
    (set? val) val
    (seq? val) val
    (number? val) val
    (keyword? val) val
    (symbol? val) val
    (= true val) val
    (= false val) val
    :else {::value (to-string val)
           ::original-type (get-type val)}))

(defn- prepare-for-serialization [m]
  (walk/prewalk prepare-for-serialization-1 m))

(defn- add-browsable-data
  "Browsable data is anything that can be displayed in the MapBrowser in the
  lower right corner of the Prone UI"
  [error]
  (if-let [data (:data error)]
    (-> error
        (assoc :browsables [{:name "Exception data", :data data}])
        (dissoc :data))
    error))

(defn- set-frame-id
  "Sometimes an exception will have multiple frames with the same location.
  In order to tell these apart in the UI, they cannot be (= a b). Adding an
  id to each achieves this."
  [idx frame]
  (assoc frame :id idx))

(defn- prep-error [error app-namespaces]
  (-> (if (:caused-by error)
        (update-in error [:caused-by] #(prep-error % app-namespaces))
        error)
      (update-in [:frames]
                 #(->> %
                       (map-indexed set-frame-id)
                       (map (partial set-application-frame app-namespaces))
                       (mapv add-source)))
      (update-in [:data] prepare-for-serialization)
      add-browsable-data))

(defn- add-browsable-debug
  "Add locals and debugged data as browsable data - data to render with the
  MapBrowser in the lower right corner of the Prone UI"
  [{:keys [forms locals] :as debug}]
  (let [browsables (concat (when locals [{:name "Local bindings", :data locals}])
                           (map #(array-map :name "Debugged data", :data %) forms))]
    (if (seq browsables)
      (assoc debug :browsables browsables)
      debug)))

(defn- prep-debug-1 [{:keys [class-path-url] :as debug}]
  (let [root-dir (str (.getCanonicalPath (File. ".")) "/")
        resource (and class-path-url (io/resource class-path-url))
        file-name (and resource (.getPath resource))]
    (-> debug
        (merge {:lang :clj
                :file-name (when file-name (str/replace file-name root-dir ""))
                :method-name "[unknown]"
                :package (and class-path-url (-> class-path-url
                                                 (str/replace #"\.[^/]*$" "")
                                                 (str/replace "/" ".")
                                                 (str/replace "_" "-")))
                :source (load-source debug)})
        add-browsable-debug)))

(defn- prep-debug [debug-data]
  (when (seq debug-data)
    (-> (mapv prep-debug-1 debug-data)
        prepare-for-serialization)))

(defn prep-error-page [error debug-data request app-namespaces]
  (let [prepped-error (prep-error error app-namespaces)
        prepped-request (prepare-for-serialization request)]
    {:title (-> prepped-error :message)
     :error prepped-error
     :debug-data (prep-debug debug-data)
     :request prepped-request
     :src-loc-selection :application
     :browsables [{:name "Request map", :data prepped-request}]}))

(defn prep-debug-page [debug-data request]
  (let [prepped-request (prepare-for-serialization request)]
    {:title "Debug halt"
     :request prepped-request
     :debug-data (prep-debug debug-data)
     :src-loc-selection :debug
     :browsables [{:name "Request map", :data prepped-request}]}))

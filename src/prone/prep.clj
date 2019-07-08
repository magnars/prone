(ns prone.prep
  "Prepare data for serialization so it can safely go over the wire, and be
  picked back up with read-string on the client."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.walk :as walk]
            [prone.code-trunc :as clj-code]
            [prone.stacks :refer [normalize-exception]]
            [realize.core :as realize])
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
  (let [s (pr-str val)
        s (if (or (.startsWith s "#<")
                  (.startsWith s "#object["))
            (.toString val)
            s)]
    (cond
      (.startsWith s "#function[clojure.core/") (second (re-find #"#function\[clojure.core/(.*?)(--\d+)?\]" s))
      (.startsWith s "#function[") (subs s 10 (dec (count s)))
      (.startsWith s (str (get-type val) "@")) ""
      :else s)))

(defn prepare-for-serialization-1 [val]
  (cond
    (nil? val) val
    (fn? val) {::value (to-string val)
               ::original-type "fn"}
    (instance? java.lang.Class val) {::value (.getName val)
                                     ::original-type "java.lang.Class"}
    (instance? clojure.lang.IRecord val) {::value (let [t (get-type val)]
                                                    (if (= "datomic.db.Db" t)
                                                      (into {} (remove (fn [[k v]] (nil? v))
                                                                       (select-keys val [:basisT :asOfT :sinceT :filt])))
                                                      (into {} val)))
                                          ::original-type (.getName (type val))}
    (instance? InputStream val) {::value (try
                                           (pr-str (slurp val))
                                           (catch IOException _ nil))
                                 ::original-type (get-type val)}
    (instance? java.net.URL val) {::value (pr-str (to-string val))
                                  ::original-type (get-type val)}
    (string? val) (let [len (count val)]
                    (if (< max-string-length len)
                      {::value (str (subs val 0 60) "...")
                       ::original-type (str "String with " len " chars")}
                      val))
    (and (map? val) (:realize.core/exception val)) (let [exception (:realize.core/exception val)]
                                                     {::value (or (.getMessage exception)
                                                                  (last (str/split (.getName (type exception)) #"\.")))
                                                      ::original-type (str "thrown-when-realized: " (get-type exception))})
    (map? val) val
    (vector? val) val
    (list? val) val
    (set? val) (conj (into [] val) :prone.prep/set?)
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

(defn maybe-update [m k f & args]
  (if (contains? m k)
    (apply update m k f args)
    m))

(defn- prep-error [error app-namespaces]
  (-> error
      (maybe-update :caused-by prep-error app-namespaces)
      (maybe-update :next prep-error app-namespaces)
      (update :frames
              #(->> %
                    (map-indexed set-frame-id)
                    (map (partial set-application-frame app-namespaces))
                    (mapv add-source)))
      (update :data (comp prepare-for-serialization realize/realize))
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
        realized-request (realize/realize request)
        prepped-request (prepare-for-serialization realized-request)]
    {:title (-> prepped-error :message)
     :location (:uri prepped-request)
     :error prepped-error
     :debug-data (prep-debug debug-data)
     :src-loc-selection :application
     :browsables [{:name "Request map", :data prepped-request}]}))

(defn prep-debug-page [debug-data request]
  (let [prepped-request (prepare-for-serialization request)]
    {:title "Debug halt"
     :location (:uri prepped-request)
     :debug-data (prep-debug debug-data)
     :src-loc-selection :debug
     :browsables [{:name "Request map", :data prepped-request}]}))

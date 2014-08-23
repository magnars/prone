(ns prone.stacks
  (:require [clojure.java.io :as io]
            [clojure.string :as str]))

(defn find-loaded-from [url]
  (when-let [path (and (io/resource url)
                       (.getPath (io/resource url)))]
    (or (second (re-find #"/([^/]+)\.jdk/" path))
        (second (re-find #"/([^/]+)\.jar!" path)))))

(defn- normalize-frame-clj [frame]
  (let [fn-name (-> frame
                    .getClassName
                    (str/replace "_" "-")
                    (str/replace #"^.*\$" ""))
        class-path-url (-> frame
                         .getClassName
                         (str/replace "." "/")
                         (str/replace #"\$.*" ".clj"))]
    {:class-path-url class-path-url
     :loaded-from (find-loaded-from class-path-url)
     :method-name (if (re-find #"^fn--\d+$" fn-name) "[fn]" fn-name)
     :package (-> frame
                  .getClassName
                  (str/replace "_" "-")
                  (str/replace #"\$.*" ""))
     :lang :clj}))

(defn- normalize-frame-java [frame]
  (let [class-path-url-stub (-> frame
                                .getClassName
                                (str/replace "." "/"))]
    {:class-path-url (str class-path-url-stub ".java")
     :loaded-from (find-loaded-from (str class-path-url-stub ".class"))
     :method-name (.getMethodName frame)
     :class-name (-> frame
                     .getClassName
                     (str/split #"\.")
                     last)
     :package (-> frame
                  .getClassName
                  (str/split #"\.")
                  butlast
                  (->> (str/join ".")))
     :lang :java}))

(defn normalize-frame [frame]
  (if-not (.getFileName frame)
    {:method-name (.getMethodName frame)
     :class-name (-> frame
                     .getClassName
                     (str/split #"\.")
                     last)
     :package (-> frame
                  .getClassName
                  (str/split #"\.")
                  butlast
                  (->> (str/join ".")))}
    (merge {:file-name (.getFileName frame)
            :line-number (.getLineNumber frame)}
           (if (-> frame .getFileName (.endsWith ".clj"))
             (normalize-frame-clj frame)
             (normalize-frame-java frame)))))

(defn normalize-exception [exception]
  (let [type (.getName (type exception))
        normalized {:message (.getMessage exception)
                    :type type
                    :class-name (last (str/split type #"\."))
                    :frames (->> exception .getStackTrace (map normalize-frame))}]
    (if-let [data (and (instance? clojure.lang.ExceptionInfo exception)
                       (.data exception))]
      (merge normalized {:data data})
      normalized)))

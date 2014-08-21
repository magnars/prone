(ns prone.stacks
  (:require [clojure.java.io :as io]
            [clojure.string :as str]))

(defn- normalize-frame-clj [frame]
  (let [fn-name (-> frame
                    .getClassName
                    (str/replace "_" "-")
                    (str/replace #"^.*\$" ""))]
    {:class-path-url (-> frame
                         .getClassName
                         (str/replace "." "/")
                         (str/replace #"\$.*" ".clj"))
     :method-name (if (re-find #"^fn--\d+$" fn-name) "[fn]" fn-name)
     :package (-> frame
                  .getClassName
                  (str/replace "_" "-")
                  (str/replace #"\$.*" ""))
     :lang :clj}))

(defn- normalize-frame-java [frame]
  {:class-path-url (-> frame
                       .getClassName
                       (str/replace "." "/")
                       (str ".java"))
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
   :lang :java})

(defn- load-source [frame]
  (assoc frame :source (if-not (:class-path-url frame)
                         "(unknown source file)"
                         (if-not (io/resource (:class-path-url frame))
                           "(could not locate source file on class path)"
                           (slurp (io/resource (:class-path-url frame)))))))

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
  {:message (.getMessage exception)
   :type (.getName (type exception))
   :frames (map load-source (->> exception
                                 .getStackTrace
                                 (map normalize-frame)))})

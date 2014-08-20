(ns prone.stacks
  (:require [clojure.string :as str]))

(defn- normalize-frame-clj [frame]
  {:class-path-url (-> frame
                       .getClassName
                       (str/replace "." "/")
                       (str/replace #"\$.*" ".clj"))
   :method-name (-> frame
                    .getClassName
                    (str/replace "_" "-")
                    (str/replace #"^.*\$" ""))
   :package (-> frame
                .getClassName
                (str/replace "_" "-")
                (str/replace #"\$.*" ""))
   :lang :clj})

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
   :type (type exception)
   :frames (->> exception
                .getStackTrace
                (map normalize-frame))})

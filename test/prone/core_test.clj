(ns prone.core-test
  (:require [prone.core :refer :all]
            [clojure.test :refer :all]))

(defn- create-ex [msg]
  (try
    (throw (Exception. msg))
    (catch Exception e
      e)))

(def ex (create-ex "Message for you, Sir!"))

(def clj-frame (->> (.getStackTrace ex)
                    (filter #(re-find #"^prone" (.getClassName %)))
                    first))

(def java-frame (->> (.getStackTrace ex)
                     (filter #(re-find #"^java.lang" (.getClassName %)))
                     first))

(deftest check-assumptions-about-exception
  (is (= "Message for you, Sir!" (.getMessage ex)))

  (is (= "prone.core_test$create_ex" (.getClassName clj-frame)))
  (is (= "invoke" (.getMethodName clj-frame)))
  (is (= "core_test.clj" (.getFileName clj-frame)))
  (is (= 5 (.getLineNumber clj-frame)))

  (is (= "java.lang.reflect.Constructor" (.getClassName java-frame)))
  (is (= "newInstance" (.getMethodName java-frame)))
  (is (= "Constructor.java" (.getFileName java-frame)))
  (is (= 526 (.getLineNumber java-frame))))

(deftest normalize-frame-test
  (is (= {:class-path-url "prone/core_test.clj"
          :file-name "core_test.clj"
          :method-name "create-ex"
          :line-number 5
          :package "prone.core-test"
          :lang :clj}
         (normalize-frame clj-frame)))

  (is (= {:class-path-url "java/lang/reflect/Constructor.java"
          :file-name "Constructor.java"
          :method-name "newInstance"
          :line-number 526
          :class-name "Constructor"
          :package "java.lang.reflect"
          :lang :java}
         (normalize-frame java-frame))))

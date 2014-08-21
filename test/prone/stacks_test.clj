(ns prone.stacks-test
  (:require [prone.stacks :refer :all]
            [clojure.test :refer :all]))

(defn- create-ex [msg]
  (try
    (throw (Exception. msg))
    (catch Exception e
      e)))

(def ex (create-ex "Message for you, Sir!"))

(def anon-fn-ex ((fn [] (create-ex "Message for you, Sir!"))))

(def clj-frame (->> (.getStackTrace ex)
                    (filter #(re-find #"^prone" (.getClassName %)))
                    first))

(def clj-anon-frame (->> (.getStackTrace anon-fn-ex)
                         (filter #(re-find #"^prone" (.getClassName %)))
                         second))

(def java-frame (->> (.getStackTrace ex)
                     (filter #(re-find #"^java.lang" (.getClassName %)))
                     first))

(deftest check-assumptions-about-exception
  (is (= "Message for you, Sir!" (.getMessage ex)))

  (is (= "prone.stacks_test$create_ex" (.getClassName clj-frame)))
  (is (= "invoke" (.getMethodName clj-frame)))
  (is (= "stacks_test.clj" (.getFileName clj-frame)))
  (is (= 5 (.getLineNumber clj-frame)))

  (is (= "java.lang.reflect.Constructor" (.getClassName java-frame)))
  (is (= "newInstance" (.getMethodName java-frame)))
  (is (= "Constructor.java" (.getFileName java-frame)))
  (is (= 526 (.getLineNumber java-frame))))

(deftest normalize-frame-test
  (is (= {:class-path-url "prone/stacks_test.clj"
          :file-name "stacks_test.clj"
          :method-name "create-ex"
          :line-number 5
          :package "prone.stacks-test"
          :lang :clj}
         (normalize-frame clj-frame)))

  (is (= {:class-path-url "java/lang/reflect/Constructor.java"
          :file-name "Constructor.java"
          :method-name "newInstance"
          :line-number 526
          :class-name "Constructor"
          :package "java.lang.reflect"
          :lang :java}
         (normalize-frame java-frame)))

  (is (= "[fn]" (:method-name (normalize-frame clj-anon-frame)))))

(deftest normalize-exception-test
  (let [normalized (normalize-exception ex)]
    (is (= "Message for you, Sir!" (:message normalized)))
    (is (= "java.lang.Exception" (:type normalized)))
    (is (= "Exception" (:class-name normalized)))))

(deftest non-file-exception
  (is (= {:method-name "handle"
          :class-name "AbstractHandler$0"
          :package "ring.adapter.jetty.proxy$org.eclipse.jetty.server.handler"}
         (normalize-frame (StackTraceElement. "ring.adapter.jetty.proxy$org.eclipse.jetty.server.handler.AbstractHandler$0" "handle" nil -1)))))

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
                     (filter #(re-find #"^clojure.lang" (.getClassName %)))
                     first))

(deftest check-assumptions-about-exception
  (is (= "Message for you, Sir!" (.getMessage ex)))

  (is (= "prone.stacks_test$create_ex" (.getClassName clj-frame)))
  (is (= "invoke" (.getMethodName clj-frame)))
  (is (= "stacks_test.clj" (.getFileName clj-frame)))
  (is (= 5 (.getLineNumber clj-frame)))

  (is (= "clojure.lang.Reflector" (.getClassName java-frame)))
  (is (= "invokeConstructor" (.getMethodName java-frame)))
  (is (= "Reflector.java" (.getFileName java-frame)))
  (is (= 180 (.getLineNumber java-frame))))

(deftest normalize-frame-test
  (is (= {:class-path-url "prone/stacks_test.clj"
          :loaded-from nil
          :file-name "stacks_test.clj"
          :method-name "create-ex"
          :line-number 5
          :package "prone.stacks-test"
          :lang :clj}
         (normalize-frame clj-frame)))

  (is (= {:class-path-url "clojure/lang/Reflector.java"
          :loaded-from "clojure-1.6.0"
          :file-name "Reflector.java"
          :method-name "invokeConstructor"
          :line-number 180
          :class-name "Reflector"
          :package "clojure.lang"
          :lang :java}
         (normalize-frame java-frame)))

  (is (= "[fn]" (:method-name (normalize-frame clj-anon-frame)))))

(deftest loaded-from-test
  (is (= "clojure-1.6.0"
         (->> (.getStackTrace ex)
              (filter #(re-find #"^clojure.lang" (.getClassName %)))
              first
              normalize-frame
              :loaded-from)))
  (is (= "clojure-1.6.0"
         (->> (.getStackTrace ex)
              (filter #(re-find #"^clojure.core" (.getClassName %)))
              first
              normalize-frame
              :loaded-from))))

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

(deftest normalize-ex-info-test
  (let [normalized (normalize-exception (try
                                          (throw (ex-info "Oops" {:data 42}))
                                          (catch Exception e
                                            e)))]
    (is (= {:data 42} (:data normalized)))))

(deftest normalize-chains-test
  (let [normalized (normalize-exception (try
                                          (throw (Exception. "It went wrong because of something else" ex))
                                          (catch Exception e
                                            e)))]
    (is (= (normalize-exception ex)
           (:caused-by normalized)))))

(deftest adding-frames-from-exception-message
  (let [normalized (normalize-exception (try
                                          (throw (Exception. "java.lang.RuntimeException: No such var: foo, compiling:(prone/stacks_test.clj:105:145)"))
                                          (catch Exception e
                                            e)))]
    (is (= {:lang :clj
            :package "prone.stacks-test"
            :method-name nil
            :loaded-from nil
            :class-path-url "prone/stacks_test.clj"
            :file-name "stacks_test.clj"
            :line-number 105}
           (first (:frames normalized))))))

(deftest don-t-add-frames-from-non-existent-files
  (let [normalized (normalize-exception (try
                                          (throw (Exception. "java.lang.RuntimeException: No such var: foo, compiling:(foo.clj:105:145)"))
                                          (catch Exception e
                                            e)))]
    (is (not= "foo"
              (-> normalized :frames first :package)))))

(deftest handle-null-message-exception
  (let [normalized (normalize-exception (try
                                          (.foo nil)
                                          (catch NullPointerException e
                                            e)))]
    (is (nil?
         (-> normalized :message)))))

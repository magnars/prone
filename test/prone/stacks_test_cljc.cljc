(ns prone.stacks-test-cljc
  (:require [prone.stacks :refer :all]
            [clojure.test :refer :all]))

(defn- create-ex [msg]
  (try
    (throw (Exception. msg))
    (catch Exception e
      e)))

(def ex (create-ex "Message for you, Sir!"))

(def anon-fn-ex (try
                  (throw (Exception. "Message for you, Sir!"))
                  (catch Exception e
                    e)))

(def clj-frame (->> (.getStackTrace ex)
                    (filter #(re-find #"^prone" (.getClassName %)))
                    first))

(def clj-anon-frame (->> (.getStackTrace anon-fn-ex)
                         (filter #(re-find #"^prone" (.getClassName %)))
                         second))

(deftest check-assumptions-about-exception
  (is (= "Message for you, Sir!" (.getMessage ex)))

  (is (= "prone.stacks_test_cljc$create_ex" (.getClassName clj-frame)))
  (is (= "invokeStatic" (.getMethodName clj-frame)))
  (is (= "stacks_test_cljc.cljc" (.getFileName clj-frame)))
  (is (= 5 (.getLineNumber clj-frame))))

(deftest normalize-frame-test
  (is (= {:class-path-url "prone/stacks_test_cljc.cljc"
          :loaded-from nil
          :file-name "stacks_test_cljc.cljc"
          :method-name "create-ex"
          :line-number 5
          :package "prone.stacks-test-cljc"
          :lang :clj}
         (normalize-frame clj-frame)))

  (is (= "[fn]" (:method-name (normalize-frame clj-anon-frame)))))

(deftest adding-frames-from-exception-message
  (let [normalized (normalize-exception (try
                                          (throw (Exception. "java.lang.RuntimeException: No such var: foo, compiling:(prone/stacks_test_cljc.cljc:105:145)"))
                                          (catch Exception e
                                            e)))]
    (is (= {:lang :clj
            :package "prone.stacks-test-cljc"
            :method-name nil
            :loaded-from nil
            :class-path-url "prone/stacks_test_cljc.cljc"
            :file-name "stacks_test_cljc.cljc"
            :line-number 105
            :column 145}
           (first (:frames normalized))))))

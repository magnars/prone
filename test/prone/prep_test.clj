(ns prone.prep-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer :all]
            [prone.prep :refer [prep-error-page prep-debug-page]])
  (:import [java.io ByteArrayInputStream]))

(defn prep-frames [frames & [application-name]]
  (-> (prep-error-page {:frames frames} {} {} application-name)
      :error :frames))

(deftest source-for-frames
  (is (re-find #"prone.prep-test"
               (:code (:source (first (prep-frames [{:class-path-url "prone/prep_test.clj"}]))))))
  (is (= "(unknown source file)"
         (:failure (:source (first (prep-frames [{}]))))))
  (is (= "(could not locate source file on class path)"
         (:failure (:source (first (prep-frames [{:class-path-url "plone/plep_test.clj"}])))))))

(deftest application-frames
  (is (= ["a"] (->> (prep-frames [{:name "a" :package "prone.prep-test"}
                                  {:name "b" :package "plone.plep-test"}]
                                 "prone")
                    (filter :application?)
                    (map :name)))))

(deftest frame-selection
  (is (= :application (:frame-selection (prep-error-page {:frames []} {} {} "")))))

(defrecord DefLeppard [num-hands])

(deftest no-unreadable-forms
  (is (= {:name "John Doe"
          :age 37
          :url {:prone.prep/value "http://example.com"
                :prone.prep/original-type "java.net.URL"}
          :body {:prone.prep/value "Hello"
                 :prone.prep/original-type "java.io.ByteArrayInputStream"}
          :lazy '(2 3 4)
          :record {:prone.prep/value {:num-hands 1}
                   :prone.prep/original-type "prone.prep_test.DefLeppard"}}
         (-> (prep-error-page {} {} {:session {:name "John Doe"
                                               :age 37
                                               :url (java.net.URL. "http://example.com")
                                               :body (ByteArrayInputStream. (.getBytes "Hello"))
                                               :lazy (map inc [1 2 3])
                                               :record (DefLeppard. 1)}} "")
             :request :session))))

(defn prep-debug [debug]
  (prep-debug-page debug {}))

(deftest prep-debug-auxilliary-info
  (let [class-path-url "prone/debug_test.clj"]

    (is (= :clj (:lang (first (:debug-data (prep-debug [{}]))))))

    (is (= "test/prone/debug_test.clj"
           (:file-name (first (:debug-data (prep-debug [{:class-path-url class-path-url}]))))))

    (is (= "prone/debug_test.clj"
           (:class-path-url (first (:debug-data (prep-debug [{:class-path-url class-path-url}]))))))

    (is (= "prone.debug-test"
           (:package (first (:debug-data (prep-debug [{:class-path-url class-path-url}]))))))

    (let [source (:source (first (:debug-data (prep-debug [{:class-path-url class-path-url}]))))]
      (is (re-find #"^\(ns prone\.debug-test" (:code source)))
      (is (= 0 (:offset source))))))

(ns prone.prep-test
  (:require [prone.prep :refer :all]
            [clojure.test :refer :all]))

(defn prep-frames [frames & [application-name]]
  (-> (prep {:frames frames} {} application-name)
      :error :frames))

(deftest ids-for-frames
  (is (= [0 1 2] (map :id (prep-frames [{} {} {}])))))

(deftest source-for-frames
  (is (re-find #"prone.prep-test"
               (:source (first (prep-frames [{:class-path-url "prone/prep_test.clj"}])))))
  (is (= "(unknown source file)"
         (:source (first (prep-frames [{}])))))
  (is (= "(could not locate source file on class path)"
         (:source (first (prep-frames [{:class-path-url "plone/plep_test.clj"}]))))))

(deftest selection-for-first-frame
  (is (= ["a"] (->> (prep-frames [{:name "a"} {:name "b"} {:name "c"}])
                    (filter :selected?)
                    (map :name)))))

(deftest application-frames
  (is (= ["a"] (->> (prep-frames [{:name "a" :package "prone.prep-test"}
                                  {:name "b" :package "plone.plep-test"}]
                                 "prone")
                    (filter :application?)
                    (map :name)))))

(deftest frame-filter
  (is (= :application (:frame-filter (prep {:frames []} {} "")))))

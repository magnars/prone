(ns prone.debug-test
  (:require [prone.debug :refer :all]
            [clojure.test :refer :all]))

(deftest debug-test
  (reset! debug-data [])

  (debug "I'm lost")
  (is (= {:line 8
          :column 3
          :message "I'm lost"
          :forms nil}
         (select-keys (last @debug-data) [:form :env :message :forms :line :column])))

  (debug {:data 42})
  (is (= [{:data 42}] (:forms (last @debug-data))))
  (is (= nil (:message (last @debug-data))))

  (debug {:data 42} {:data 13})
  (is (= [{:data 42} {:data 13}] (:forms (last @debug-data))))

  (let [local 42
        binding 13]
    (debug "Help!")
    (is (= {'local 42, 'binding 13} (:locals (last @debug-data))))))

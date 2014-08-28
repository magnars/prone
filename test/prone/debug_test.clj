(ns prone.debug-test
  (:require [prone.debug :refer :all]
            [clojure.test :refer :all]))

(deftest debug-test
  (binding [*debug-data* (atom [])]

    (debug "I'm lost")
    (is (= {:id 0
            :line-number 8
            :column 5
            :message "I'm lost"
            :forms nil
            :locals nil
            :class-path-url "prone/debug_test.clj"}
           (select-keys (last @*debug-data*)
                        [:id :form :env :message :forms :line-number :column :locals :class-path-url])))

    (debug {:data 42})
    (is (= [{:data 42}] (:forms (last @*debug-data*))))
    (is (= nil (:message (last @*debug-data*))))

    (debug {:data 42} {:data 13})
    (is (= [{:data 42} {:data 13}] (:forms (last @*debug-data*))))

    (let [local 42
          binding 13]
      (debug "Help!")
      (is (= {'local 42, 'binding 13} (:locals (last @*debug-data*)))))

    (debug (str "H" "e" "y"))
    (is (= ["Hey"] (:forms (last @*debug-data*))))))

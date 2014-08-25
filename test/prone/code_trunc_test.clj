(ns prone.code-trunc-test
  (:require [prone.code-trunc :refer :all]
            [clojure.test :refer :all]))

(deftest truncate-short-code
  (is (= {:code "(prn 42)" :offset 0} (truncate "(prn 42)" 1 500))))

(deftest truncate-long-code
  (is (= {:code "(prn\n  42)", :offset 0} (truncate "(prn\n  42)\n\n(prn 13)" 1 2)))
  (is (= {:code "(prn 13)", :offset 3} (truncate "(prn\n 42)\n\n(prn 13)" 4 2)))

  (let [fn-str "(defn doit []\n  (prn \"Yay\")\n  (prn \"Nay\"))"]
    (is (= fn-str
           (:code (truncate (str "(prn \"Before\")\n\n" fn-str "\n\n(prn \"After\")") 5 5))))

    (is (= fn-str
           (:code (truncate (str fn-str "\n\n(prn \"After\")") 2 5))))

    (is (= fn-str
           (:code (truncate (str "(prn \"Before\")\n\n" fn-str) 5 5))))))

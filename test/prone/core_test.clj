(ns prone.core-test
  (:require [prone.core :refer :all]
            [prone.debug :refer [debug debug-data]]
            [clojure.test :refer :all]))

(deftest resets-debug-on-every-request-test
  ((wrap-exceptions (fn [req] {})) {})
  (is (= [] @debug-data))

  ((wrap-exceptions (fn [req]
                      (debug "Oh noes")
                      {})) {})
  (is (= ["Oh noes"] (map :message @debug-data)))

  ((wrap-exceptions (fn [req]
                      (debug "Oh noes")
                      (debug "Halp!")
                      {})) {})
  (is (= ["Oh noes" "Halp!"] (map :message @debug-data))))

(deftest renders-debug-page-on-debug
  (is (= 203 (:status ((wrap-exceptions (fn [req]
                                          (debug "I need help")
                                          {:status 200})) {})))))

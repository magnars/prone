(ns prone.middleware-test
  (:require [clojure.test :refer :all]
            [prone.debug :refer [debug *debug-data*]]
            [prone.middleware :refer :all]))

(deftest resets-debug-on-every-request-test

  (declare inner-debug-data)

  ((wrap-exceptions (fn [req] (def inner-debug-data @*debug-data*))) {})
  (is (= [] inner-debug-data))

  ((wrap-exceptions (fn [req]
                      (debug "Oh noes")
                      (def inner-debug-data @*debug-data*))) {})
  (is (= ["Oh noes"] (map :message inner-debug-data)))

  ((wrap-exceptions (fn [req]
                      (debug "Oh noes")
                      (debug "Halp!")
                      (def inner-debug-data @*debug-data*))) {})
  (is (= ["Oh noes" "Halp!"] (map :message inner-debug-data))))

(defn test-handler [handler]
  ((wrap-exceptions handler {:print-stacktraces? false}) {}))

(deftest catches-exceptions-and-assertion-errors
  (let [response (test-handler (fn [req] (/ 1 0)))]
    (is (= 500 (:status response)))
    (is (re-find #"prone-data" (:body response))))
  (let [response (test-handler (fn [req] (assert false)))]
    (is (= 500 (:status response)))
    (is (re-find #"prone-data" (:body response))))
  (is (thrown? Error
               (test-handler (fn [req] (throw (Error.)))))))

(deftest renders-debug-page-on-debug
  (is (= 203 (:status ((wrap-exceptions (fn [req]
                                          (debug "I need help")
                                          {:status 200})) {})))))

(deftest excludes-unwanted-clients
  (are [status headers] (= status (:status
                                   ((wrap-exceptions
                                     (fn [req]
                                       (debug "I need help")
                                       {:status 200})
                                     {:skip-prone? (fn [req]
                                                     (contains? (:headers req) "postman-token"))})
                                    {:headers headers})))
    200 {"postman-token" "12345"
         "other" "value"}
    203 {"random" "string"}
    203 {}))

(defn async-test-handler [handler]
  (let [response (atom nil)]
    ((wrap-exceptions handler {:print-stacktraces? false})
     {}
     (partial reset! response)
     (fn []))
    @response))

(deftest async-catches-exceptions-and-assertion-errors
  (let [response (async-test-handler (fn [req respond raise]
                                       (try
                                         (/ 1 0)
                                         (catch Exception e
                                           (raise e)))))]
    (is (= 500 (:status response)))
    (is (re-find #"prone-data" (:body response))))
  (let [response (async-test-handler (fn [req respond raise]
                                       (/ 1 0)))]
    (is (= 500 (:status response)))
    (is (re-find #"prone-data" (:body response))))
  (let [response (test-handler (fn [req respond raise]
                                 (try
                                   (assert false)
                                   (catch AssertionError e
                                     (raise e)))))]
    (is (= 500 (:status response)))
    (is (re-find #"prone-data" (:body response)))))

(deftest async-renders-debug-page-on-debug
  (is (= 203
         (-> (async-test-handler (fn [req respond raise]
                                   (debug "I need help")
                                   (respond {:status 200})))
             :status))))

(deftest finds-application-name
  (is (= 'prone (find-application-name-in-project-clj "(defproject prone ...)")))
  (is (= 'parens-of-the-dead (find-application-name-in-project-clj "(defproject parens-of-the-dead ...)")))
  (is (= 'prone (find-application-name-in-project-clj "
                                                        (defproject
                                                          prone ...)")))
  (is (= nil (find-application-name-in-project-clj "(def prone ...)")))
  (is (= nil (find-application-name-in-project-clj "(defprojectprone ...)"))))

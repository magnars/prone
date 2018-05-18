(ns prone.demo
  (:require [clojure.java.io :as io]
            [datomic.api :as d]
            [prone.debug :refer [debug]]
            [prone.middleware :refer [wrap-exceptions]]
            [hiccup.core :as h])
  (:import [java.io ByteArrayInputStream]
           (java.sql SQLException)))

(defrecord MyRecord [num])

(deftype MyType [num])

(def conn (do (d/create-database "datomic:mem://test-db")
              (d/connect "datomic:mem://test-db")))

(defn piggie-backer [app]
  (fn [req]
    (app (-> req
             (assoc :nested {:maps {:are {:certainly {:supported 42, :displayed 43}}}})
             (assoc :some {:nested {:maps {:are {:simply "Too long to peek
             inside, at least while staying on only a single line.
             They need to be shortened somehow in the UI."}}}})
             (assoc :vectors [:are "Also" "supported" 13])
             (assoc :lists '(:are "Also" "supported" 13))
             #_(assoc :lots-and-lots (into {}
                                         (for [i (range 2000)]
                                           [i (map (fn [_] (throw (Exception. (str "Surprise #" i "!")))) [1 2 3])])))
             (assoc :exceptions {:inside {:lazy-lists (map (fn [_] (throw (Exception. "Surprise!"))) [1 2 3])
                                          :are-handled (map name [:foo :bar nil])}})
             (assoc :sets #{:are "Also" "supported" 13})
             (assoc :some-vectors ["are" "simply" "too" "long" "to" "peek" "inside,"
                                   "at" "least" "while" "staying" "on" "only" "a" "single" "line."
                                   "They need to be shortened somehow in the UI."])
             (assoc :namespaced-maps #:my-ns{:are "supported"
                                             :as "well"})
             (assoc :datomic (let [db (d/db conn)]
                               {:conn conn :db db :entity (d/entity db 1)}))
             (assoc :now (java.util.Date.))
             (assoc :session {:name "John Doe"
                              :age 37
                              :url (java.net.URL. "http://example.com")
                              :body (ByteArrayInputStream. (.getBytes "Hello"))
                              :lazy (map inc [1 2 3])
                              :record (MyRecord. 17)
                              :type (MyType. 23)})))))

(defn- create-original-cause []
  (try
    (throw (Exception. "This is the original cause"))
    (catch Exception e
      e)))

(defn- create-intermediate-cause []
  (try
    (throw (Exception. "This is an intermediate cause" (create-original-cause)))
    (catch Exception e
      e)))

(defn handler [req]
  (cond
    ;; throw exception in dependency (outside of app)
    (= (:uri req) "/external-throw") (re-find #"." nil)

    ;; throw an ex-info with data attached
    (= (:uri req) "/ex-info")
    (throw (ex-info "This will be informative" {:authors [:magnars :cjohansen]
                                                :deep {:dark {:scary {:nested {:data "Structure"
                                                                               :of "Doom"
                                                                               :fruits ["Apple" "Orange" "Pear" "Banana" "Peach" "Watermelon" "Guava"]}}}}}))

    (= (:uri req) "/lazy")
    (throw (ex-info "This lazy seq has an exception in store"
                    {:dangerously {:lazy (map (fn [_] (throw (Exception. "Exception inside a lazy list"))) [1 2 3])}}))

    ;; throw an exception with a cause
    (= (:uri req) "/caused-by")
    (throw (Exception. "It went wrong because of something else" (create-intermediate-cause)))

    ;; throw an SQLException with a cause
    (= (:uri req) "/sql")
    (throw (doto (SQLException. "Has a next exception")
             (.initCause (create-intermediate-cause))
             (.setNextException
               (doto (SQLException. "Next exception!")
                 (.setNextException (SQLException. "Next next exception!!"))))))


    ;; use the debug function to halt rendering (and inspect data)
    (= (:uri req) "/debug")
    (do
      (debug "How's this work?" {:id (rand)})
      (debug {:id (rand)})

      (let [team "America"]
        (debug "Look at them locals"))

      {:status 200
       :headers {"content-type" "text/html"}
       :body "<h1>Hello, bittersweet and slightly tangy world</h1>"})

    ;; basic case
    (= (:uri req) "/basic")
    (do
      ;; A map with nil as key, that is too big to render inline
      ;; (Used to cause a bug in the MapBrowser)
      (debug {nil [1 2 3 4 5 6 7 8 9 10
                   1 2 3 4 5 6 7 8 9 10
                   1 2 3 4 5 6 7 8 9 10
                   1 2 3 4 5 6 7 8 9 10
                   1 2 3 4 5 6 7 8 9 10
                   1 2 3 4 5 6 7 8 9 10
                   1 2 3 4 5 6 7 8 9 10
                   1 2 3 4 5 6 7 8 9 10
                   1 2 3 4 5 6 7 8 9 10
                   1 2 3 4 5 6 7 8 9 10]})
      (debug "What's this" {:id 42 :req req})
      (throw (Exception. "Oh noes!")))

    ;; serve source maps
    (re-find #"prone.js.map$" (:uri req))
    {:status 200
     :headers {"Content-Type" "application/octet-stream"}
     :body (slurp (io/resource "prone/generated/prone.js.map"))}

    :else
    {:status 200
     :headers {"content-type" "text/html"}
     :body (h/html
             [:h2 "Examples:"]
             (into [:ul]
                   (for [x ["external-throw" "ex-info" "lazy" "caused-by" "sql" "debug" "basic"]]
                     [:li [:a {:href x} x]])))}))

(def app
  (-> handler
      wrap-exceptions
      piggie-backer))

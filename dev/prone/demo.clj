(ns prone.demo
  (:require [clojure.java.io :as io]
            [prone.core :refer [wrap-exceptions]]
            [prone.debug :refer [debug]]
            [schema.core :refer [optional-key either validate Str Num Keyword]])
  (:import [java.io ByteArrayInputStream]))

(defrecord MyRecord [num])

(deftype MyType [num])

(defn piggie-backer [app]
  (fn [req]
    (app (-> req
             (assoc :nested {:maps {:are {:certainly {:supported 42, :displayed 43}}}})
             (assoc :some {:nested {:maps {:are {:simply "Too long to peek
             inside, at least while staying on only a single line.
             They need to be shortened somehow in the UI."}}}})
             (assoc :vectors [:are "Also" "supported" 13])
             (assoc :lists '(:are "Also" "supported" 13))
             (assoc :sets #{:are "Also" "supported" 13})
             (assoc :some-vectors ["are" "simply" "too" "long" "to" "peek" "inside,"
                                   "at" "least" "while" "staying" "on" "only" "a" "single" "line."
                                   "They need to be shortened somehow in the UI."])
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

   ;; serve source maps
   (re-find #"prone.js.map$" (:uri req))
   {:status 200
    :headers {"Content-Type" "application/octet-stream"}
    :body (slurp (io/resource "prone/generated/prone.js.map"))}

   ;; throw exception in dependency (outside of app)
   (= (:uri req) "/external-throw") (re-find #"." nil)

   ;; throw an ex-info with data attached
   (= (:uri req) "/ex-info")
   (throw (ex-info "This will be informative" {:authors [:magnars :cjohansen]
                                               :deep {:dark {:scary {:nested {:data "Structure"
                                                                              :of "Doom"
                                                                              :fruits ["Apple" "Orange" "Pear" "Banana" "Peach" "Watermelon" "Guava"]}}}}}))

   ;; throw an exception with a cause
   (= (:uri req) "/caused-by")
   (throw (Exception. "It went wrong because of something else" (create-intermediate-cause)))

   ;; prismatic/schema validation error
   (= (:uri req) "/schema")
   (validate {:name Str, (optional-key :age) Num, :id (either Str Keyword)}
             {:name 17, :id 18})

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
   :else (do
           (debug "What's this" {:id 42 :req req})
           (throw (Exception. "Oh noes!")))))

(def app
  (-> handler
      wrap-exceptions
      piggie-backer))

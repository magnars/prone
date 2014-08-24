(ns prone.demo
  (:require [clojure.java.io :as io]
            [prone.core :refer [wrap-exceptions]])
  (:import [java.io ByteArrayInputStream]))

(defn piggie-backer [app]
  (fn [req]
    (app (-> req
             (assoc :nested {:maps {:are {:certainly {:supported 42}}}})
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
                              :lazy (map inc [1 2 3])})))))

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
   (throw (ex-info "This will be informative" {:authors [:magnars :cjohansen]}))

   ;; basic case
   :else (throw (Exception. "Oh noes!"))))

(def app
  (-> handler
      wrap-exceptions
      piggie-backer))

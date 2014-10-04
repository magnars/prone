(ns prone.middleware
  "The Prone middleware can be used to catch exceptions from your app and
  present them in an interactive explorer UI. Stack traces will by default
  be filtered to focus on your app's stack frames, and environment data is
  available for inspection, along with data passed to Prone via
  prone.debug/debug"
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [prone.debug :as debug]
            [clojure.string :as str]
            [prone.hiccough :refer [render]]
            [prone.prep :refer [prep-error-page prep-debug-page]]
            [prone.stacks :refer [normalize-exception]]))

(defn- serve [html & [status]]
  {:status (or status 500)
   :body html
   :headers {"Content-Type" "text/html"}})

(defn- get-application-name
  "Assume that prone is being used from a leiningen project, and fetch the
  application name from project.clj - this allows prone to differentiate
  application frames from libraries/runtime frames."
  []
  (second (edn/read-string (slurp "project.clj"))))

(defn- random-string-not-present-in
  "Look for a random string that is not present in haystack, increasing the
   length for each failed attempt."
  [haystack length]
  (let [needle (->> #(rand-nth "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ")
                    repeatedly
                    (take length)
                    (apply str))]
    (if (.contains haystack needle)
      (recur haystack (inc length))
      needle)))

(defn- render-page [data]
  (render
   (let [data-str (prn-str data)
         script-replacement-string (random-string-not-present-in data-str 6)]
     (list "<!DOCTYPE html>"
           [:html
            [:head
             [:title (:title data)]
             [:style (slurp (io/resource "prone/better-errors.css"))]
             [:style (slurp (io/resource "prismjs/themes/prism.css"))]
             [:style (slurp (io/resource "prismjs/plugins/line-highlight/prism-line-highlight.css"))]
             [:style (slurp (io/resource "prismjs/plugins/line-numbers/prism-line-numbers.css"))]
             [:style (slurp (io/resource "prone/styles.css"))]]
            [:body
             [:div {:id "ui-root"}]
             [:input {:type "hidden" :id "script-replacement-string" :value script-replacement-string}]
             [:script {:type "text/json" :id "prone-data"} (str/replace data-str #"\bscript\b" script-replacement-string)]
             [:script (slurp (io/resource "node_modules/react/dist/react.min.js"))]
             [:script (slurp (io/resource "prismjs/prism.js"))]
             [:script (slurp (io/resource "prismjs/plugins/line-numbers/prism-line-numbers.min.js"))]
             [:script (slurp (io/resource "prismjs/plugins/line-highlight/prism-line-highlight.min.js"))]
             [:script (slurp (io/resource "prone/prism-line-numbers.js"))]
             [:script (slurp (io/resource "prism-clojure/prism.clojure.js"))]
             [:script (slurp (io/resource "prone/generated/prone.js"))]]]))))

(defn wrap-exceptions
  "Let Prone handle exeptions instead of Ring. This way, instead of a centered
   stack trace, errors will give you a nice interactive page where you can browse
   data, filter the stack trace and generally get a good grip of what is
   happening.

   Optionally, supply a opts map to specify namespaces to include, E.G.,

   => (wrap-exceptions handler {:app-namespaces ['your-ns-1 'my.ns.to-show]})"
  [handler & [{:keys [app-namespaces] :as opts}]]
  (fn [req]
    (binding [debug/*debug-data* (atom [])]
      (try
        (let [result (handler req)]
          (if (< 0 (count @debug/*debug-data*))
            (-> @debug/*debug-data*
                (prep-debug-page req)
                render-page
                (serve 203))
            result))
        (catch Exception e
          (.printStackTrace e)
          (-> e
              normalize-exception
              (prep-error-page @debug/*debug-data*
                               req
                               (or app-namespaces [(get-application-name)]))
              render-page
              serve))))))

(ns prone.core
  (:require [clojure.java.io :as io]
            [clojure.string :as str]))

(defn- render-attr [[k v]]
  (str " " (name k) "=\"" v "\""))

(defn- tag [type attrs & contents]
  (str "<" (name type) (str/join (map render-attr attrs)) ">"
       (str/join (flatten contents))
       "</" (name type) ">"))

(def p (partial tag :p))
(def h1 (partial tag :h1))
(def h2 (partial tag :h2))
(def h3 (partial tag :h3))
(def div (partial tag :div))
(def span (partial tag :span))
(def strong (partial tag :strong))
(def header (partial tag :header))
(def section (partial tag :section))
(def nav (partial tag :nav))
(def ul (partial tag :ul))
(def li (partial tag :li))
(def a (partial tag :a))

(defn- serve [html]
  {:status 200
   :body html
   :headers {"Content-Type" "text/html"}})

(defn with-layout [title & html-forms]
  (str "<!DOCTYPE html>"
       (tag :html {}
            (tag :head {}
                 (tag :title {} title)
                 (tag :style {} (slurp (io/resource "prone-styles.css"))))
            (tag :body {} (str/join html-forms)))))

(defn render-stack-frame [frame]
  (li {}
      (span {:class "stroke"}
            (span {:class "icon"})
            (div {:class "info"}
                 (div {:class "name"}
                      (strong {} (.getClassName frame))
                      (span {:class "method"} "#" (.getMethodName frame)))
                 (div {:class "location"}
                      (span {:class "filename"}
                            (.getFileName frame))
                      ", line "
                      (span {:class "line"} (.getLineNumber frame)))))))

(defn serve-exception [req e]
  (serve
   (with-layout (.getMessage e)
     (div {:class "top"}
          (header {:class "exception"}
                  (h2 {} (strong {} (type e)) (span {} " at " (:uri req)))
                  (p {} (.getMessage e))))
     (section {:class "backtrace"}
              (nav {:class "sidebar"}
                   (nav {:class "tabs"}
                        (a {:href "#"} "Application Frames")
                        (a {:href "#" :class "selected"} "All Frames"))
                   (ul {:class "frames"}
                       (map render-stack-frame (.getStackTrace e))))))))

(defn wrap-exceptions [handler]
  (fn [req]
    (try
      (handler req)
      (catch Exception e
        (serve-exception req e)))))

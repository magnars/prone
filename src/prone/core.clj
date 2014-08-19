(ns prone.core
  (:require [clojure.java.io :as io]
            [clojure.string :as str]))

(defn- normalize-frame-clj [frame]
  {:class-path-url (-> frame
                       .getClassName
                       (str/replace "." "/")
                       (str/replace #"\$.*" ".clj"))
   :method-name (-> frame
                    .getClassName
                    (str/replace "_" "-")
                    (str/replace #"^.*\$" ""))
   :package (-> frame
                .getClassName
                (str/replace "_" "-")
                (str/replace #"\$.*" ""))
   :lang :clj})

(defn- normalize-frame-java [frame]
  {:class-path-url (-> frame
                       .getClassName
                       (str/replace "." "/")
                       (str ".java"))
   :method-name (.getMethodName frame)
   :class-name (-> frame
                   .getClassName
                   (str/split #"\.")
                   last)
   :package (-> frame
                .getClassName
                (str/split #"\.")
                butlast
                (->> (str/join ".")))
   :lang :java})

(defn normalize-frame [frame]
  (merge {:file-name (.getFileName frame)
          :line-number (.getLineNumber frame)}
         (if (-> frame .getFileName (.endsWith ".clj"))
           (normalize-frame-clj frame)
           (normalize-frame-java frame))))

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

(defn- get-class-path-file [class-name]
  (-> class-name
      (str/replace "." "/")
      (str/replace #"\$.+$" "")
      (str ".clj")))

(defn render-frame-info [frame]
  (div {:class "frame_info"}
       (header {:class "trace_info clearfix"}
               (div {:class "title"}
                    (h2 {:class "name"} (.getMethodName frame))
                    (div {:class "location"}
                         (span {:class "filename"}
                               (.getFileName frame))))
               (div {:class "code_block clearfix"}
                    (tag :pre {}
                         (slurp (io/resource (get-class-path-file (.getClassName frame)))))))))

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
                       (map render-stack-frame (.getStackTrace e))))
              (render-frame-info (first (.getStackTrace e)))))))

(defn wrap-exceptions [handler]
  (fn [req]
    (try
      (handler req)
      (catch Exception e
        (serve-exception req e)))))

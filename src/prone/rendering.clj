(ns prone.rendering
  "Functions to render exception and request/response data as HTML"
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [prone.hiccough :refer :all]))

(defn with-layout [title & markup-snippets]
  (str "<!DOCTYPE html>"
       (tag :html {}
            (tag :head {}
                 (tag :title {} title)
                 (tag :style {} (slurp (io/resource "prone-styles.css"))))
            (tag :body {} (str/join markup-snippets)))))

(defn build-stack-frame [frame]
  [:li
   [:span {:class "stroke"}
    [:span {:class "icon"}]
    [:div {:class "info"}
     (if (:clj? frame)
       [:div {:class "name"}
        [:strong (:package frame)]
        [:span {:class "method"} "/" (:method-name frame)]]
       [:div {:class "name"}
        [:strong (:package frame) "." (:class-name frame)]
        [:span {:class "method"} "$" (:method-name frame)]])
     (if (:file-name frame)
       [:div {:class "location"}
        [:span {:class "filename"}
         (:file-name frame)]
        ", line "
        [:span {:class "line"} (:line-number frame)]]
       [:div {:class "location"}
        "(unknown file)"])]]])

(defn build-frame-info [frame]
  (div {:class "frame_info"}
       (header {:class "trace_info clearfix"}
               (div {:class "title"}
                    (h2 {:class "name"} (:method-name frame))
                    (div {:class "location"}
                         (span {:class "filename"}
                               (:class-path-url frame))))
               (div {:class "code_block clearfix"}
                    (tag :pre {}
                         (slurp (io/resource (:class-path-url frame))))))))

(defn render-exception [request {:keys [message type frames]}]
  (with-layout message
    (div {:class "top"}
         (header {:class "exception"}
                 (h2 {} (strong {} type) (span {} " at " (:uri request)))
                 (p {} message)))
    (section {:class "backtrace"}
             (nav {:class "sidebar"}
                  (nav {:class "tabs"}
                       (a {:href "#"} "Application Frames")
                       (a {:href "#" :class "selected"} "All Frames"))
                  (ul {:class "frames"}
                      (map (comp render build-stack-frame) frames)))
             (build-frame-info (first frames)))))

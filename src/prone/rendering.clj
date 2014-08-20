(ns prone.rendering
  "Functions to render exception and request/response data as HTML"
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [prone.hiccough :refer :all]))

(defn with-layout [title & markup-snippets]
  (list "<!DOCTYPE html>"
        [:html
         [:head
          [:title title]
          [:style (slurp (io/resource "prone-styles.css"))]]
         [:body markup-snippets]]))

(defn build-stack-frame [frame]
  [:li
   [:span {:class "stroke"}
    [:span {:class "icon"}]
    [:div {:class "info"}
     (if (= (:lang frame) :clj)
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
  [:div {:class "frame_info"}
   [:header {:class "trace_info clearfix"}
    [:div {:class "title"}
     [:h2 {:class "name"} (:method-name frame)]
     [:div {:class "location"}
      [:span {:class "filename"}
       (:class-path-url frame)]]]
    [:div {:class "code_block clearfix"}
     [:pre
      (if-not (:class-path-url frame)
        "(unknown source file)"
        (if-not (io/resource (:class-path-url frame))
          "(could not locate source file on class path)"
          (slurp (io/resource (:class-path-url frame)))))]]]])

(defn build-exception [request {:keys [message type frames]}]
  (list [:div {:class "top"}
         [:header {:class "exception"}
          [:h2 [:strong type] [:span " at " (:uri request)]]
          [:p message]]]
        [:section {:class "backtrace"}
         [:nav {:class "sidebar"}
          [:nav {:class "tabs"}
           [:a {:href "#"} "Application Frames"]
           [:a {:href "#" :class "selected"} "All Frames"]]
          [:ul {:class "frames"}
           (map build-stack-frame frames)]]
         (build-frame-info (first frames))]))

(defn render-exception [request {:keys [message] :as error}]
  (render
   (with-layout message (build-exception request error))))

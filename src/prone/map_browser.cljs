(ns prone.map-browser
  (:require [cljs.core.async :refer [put! chan <!]]
            [clojure.string :as str]
            [prone.uitilities :refer [action]]
            [quiescent :as q :include-macros true]
            [quiescent.dom :as d]))

(defn- to-str [v]
  (str/trim (prn-str v)))

(defn- get-token-class
  "These token classes are recognized by Prism.js, giving values in the map
   browser similar highlighting as the source code."
  [v]
  (str "token "
       (cond
        (string? v) "string"
        (number? v) "number"
        (keyword? v) "operator")))

(q/defcomponent MapSummary
  "A map summary is a list of its keys enclosed in brackets. The summary is
   given the comment token type to visually differentiate it from fully expanded
   maps"
  [ks]
  (let [linked-keys (interpose " " (map #(d/span {:className "token comment"} (to-str %)) ks))]
    (apply d/pre {} (flatten ["{" linked-keys "}"]))))

(defn browseworthy-map?
  "Maps are only browseworthy if it is inconvenient to just look at the inline
   version (i.e., it is too big)"
  [m]
  (and (map? m)
       (< 100 (.-length (to-str m)))))

;; The gen-map-entry function is used by functions itself calls. Circular
;; dependencies are nasty, and one day I am sure we can avoid this one as well.
(declare gen-map-entry)

(q/defcomponent ValueToken
  "A simple value, render it with its type so it gets highlighted"
  [t]
  (d/code {:className (get-token-class t)} (to-str t)))

(defn- format-inline-map [m navigate-request]
  (let [[k v] (gen-map-entry m navigate-request)]
    ["{" k " " v "}"]))

(q/defcomponent InlineMapBrowser
  "Display the map all in one line. The name implies browsability - this
   unfortunately is not in place yet. Work in progress."
  [m navigate-request]
  (let [kv-pairs (mapcat #(format-inline-map % navigate-request) m)]
    (apply d/span {} kv-pairs)))

(q/defcomponent InlineVectorBrowser
  "Display a vector in one line"
  [v navigate-request]
  (apply d/span {} (flatten ["[" (interpose " " (map InlineToken v)) "]"])))

(q/defcomponent InlineToken
  "A value to be rendered roughly in one line. If the value is a list or a
   map, it will be browsable as well"
  [t navigate-request]
  (cond
   (map? t) (InlineMapBrowser t navigate-request)
   (vector? t) (InlineVectorBrowser t navigate-request)
   :else (ValueToken t)))

(defn gen-map-entry [[k v] navigate-request]
  (cond
   (browseworthy-map? v) [(d/a {:href "#"
                                :onClick (action #(put! navigate-request [:concat [k]]))} (to-str k))
                          (MapSummary (keys v))]
   :else [(ValueToken k) (InlineToken v navigate-request)]))

(q/defcomponent MapEntry
  "A map entry is one key/value pair, formatted apropriately for their types"
  [m navigate-request]
  (let [[k v] (gen-map-entry m navigate-request)]
    (d/tr {}
          (d/td {:className "name"} k)
          (d/td {} v))))

(q/defcomponent MapPath
  "The heading and current path in the map. When browsing nested maps and lists,
   the path component will display the full path from the root of the map, with
   navigation options along the way."
  [path navigate-request]
  (let [paths (map #(take (inc %) path) (range (count path)))]
    (apply d/span {}
           (conj
            (mapv #(d/a {:href "#"
                         :onClick (action (fn [] (put! navigate-request [:reset %])))}
                        (to-str (last %)))
                  (butlast paths))
            (when-let [curr (last (last paths))]
              (to-str curr))))))

(q/defcomponent MapBrowser [{:keys [name data path]} navigate-request]
  (d/div
   {}
   (d/h3 {}
         (if (empty? path) name (d/a {:href "#"
                                      :onClick (action #(put! navigate-request [:reset []]))} name))
         " "
         (d/span {:className "subtle"}
                 (MapPath path navigate-request)))
   (d/div {:className "inset variables"}
          (d/table {:className "var_table"}
                   (apply d/tbody {}
                          (map #(MapEntry % navigate-request) (get-in data path)))))))

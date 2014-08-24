(ns prone.map-browser
  (:require [cljs.core.async :refer [put! chan <!]]
            [clojure.string :as str]
            [prone.uitilities :refer [action]]
            [quiescent :as q :include-macros true]
            [quiescent.dom :as d]))

(def inline-length-limit 125)

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

(declare InlineToken)

(q/defcomponent ValueToken
  "A simple value, render it with its type so it gets highlighted"
  [t]
  (d/code {:className (get-token-class t)} (to-str t)))

(q/defcomponent SerializedValueToken
  [t]
  (d/code {:className (get-token-class (:prone.prep/to-string t))}
          (prn-str (:prone.prep/to-string t))
          (d/span {:className "subtle"} "<" (:prone.prep/original-type t) ">")))

(defn- format-inline-map [[k v] navigate-request]
  [(InlineToken k navigate-request) " " (InlineToken v navigate-request)])

(q/defcomponent InlineMapBrowser
  "Display the map all in one line. The name implies browsability - this
   unfortunately is not in place yet. Work in progress."
  [m navigate-request]
  (let [kv-pairs (mapcat #(format-inline-map % navigate-request) m)]
    (apply d/span {} (concat ["{"] (interpose " " kv-pairs) ["}"]))))

(defn- format-list [l pre post]
  (apply d/span {} (flatten [pre (interpose " " (map InlineToken l)) post])))

(q/defcomponent InlineVectorBrowser
  "Display a vector in one line"
  [v navigate-request]
  (format-list v "[" "]"))

(q/defcomponent InlineListBrowser
  "Display a list in one line"
  [v navigate-request]
  (format-list v "(" ")"))

(q/defcomponent InlineSetBrowser
  "Display a set in one line"
  [v navigate-request]
  (format-list v "#{" "}"))

(defn serialized-value? [v]
  (:prone.prep/original-type v))

(q/defcomponent InlineToken
  "A value to be rendered roughly in one line. If the value is a list or a
   map, it will be browsable as well"
  [t navigate-request]
  (cond
   (serialized-value? t) (SerializedValueToken t)
   (map? t) (InlineMapBrowser t navigate-request)
   (vector? t) (InlineVectorBrowser t navigate-request)
   (list? t) (InlineListBrowser t navigate-request)
   (set? t) (InlineSetBrowser t navigate-request)
   :else (ValueToken t)))

(defn browseworthy-map?
  "Maps are only browseworthy if it is inconvenient to just look at the inline
  version (i.e., it is too big)"
  [m]
  (and (map? m)
       (not (serialized-value? m))
       (< inline-length-limit (.-length (to-str m)))))

(defn browseworthy-list?
  "Lists are only browseworthy if it is inconvenient to just look at the inline
  version (i.e., it is too big)"
  [t]
  (and (or (list? t) (vector? t))
       (< inline-length-limit (.-length (to-str t)))))

(q/defcomponent MapSummary
  "A map summary is a list of its keys enclosed in brackets. The summary is
   given the comment token type to visually differentiate it from fully expanded
   maps"
  [k ks navigate-request]
  (let [too-long? (< inline-length-limit (.-length (to-str ks)))
        linked-keys (if too-long?
                      (str (count ks) " keys")
                      (interpose " " (map #(d/span {} (to-str %)) ks)))]
    (d/a {:href "#"
          :onClick (action #(put! navigate-request [:concat [k]]))}
         (apply d/pre {} (flatten ["{" linked-keys "}"])))))

(q/defcomponent ListSummary
  [k v navigate-request]
  (d/a {:href "#"
        :onClick (action #(put! navigate-request [:concat [k]]))}
       (cond
        (list? v) (d/pre {} "(" (count v) " items)")
        (vector? v) (d/pre {} "[" (count v) " items]"))))

(q/defcomponent MapEntry
  "A map entry is one key/value pair, formatted appropriately for their types"
  [[k v] navigate-request]
  (d/tr {}
        (d/td {:className "name"} (InlineToken k navigate-request))
        (d/td {} (cond
                  (browseworthy-map? v) (MapSummary k (keys v) navigate-request)
                  (browseworthy-list? v) (ListSummary k v navigate-request)
                  :else (InlineToken v navigate-request)))))

(q/defcomponent MapPath
  "The heading and current path in the map. When browsing nested maps and lists,
   the path component will display the full path from the root of the map, with
   navigation options along the way."
  [path navigate-request]
  (let [paths (map #(take (inc %) path) (range (count path)))]
    (apply d/span {}
           (interpose " "
                      (conj
                       (mapv #(d/a {:href "#"
                                    :onClick (action (fn [] (put! navigate-request [:reset %])))}
                                   (to-str (last %)))
                             (butlast paths))
                       (when-let [curr (last (last paths))]
                         (to-str curr)))))))

(defn- get-in* [data path]
  "Like get-in, but looks up indexed values in lists too."
  (loop [data data
         [head & tail] path]
    (if head
      (recur (if (and (list? data)
                      (number? head))
               (nth data head)
               (get data head))
             tail)
      data)))

(q/defcomponent MapBrowser [{:keys [name data path]} navigate-request]
  (d/div
   {}
   (d/h3 {:className "map-path"}
         (if (empty? path) name (d/a {:href "#"
                                      :onClick (action #(put! navigate-request [:reset []]))} name))
         " "
         (MapPath path navigate-request))
   (d/div {:className "inset variables"}
          (d/table {:className "var_table"}
                   (apply d/tbody {}
                          (let [view (get-in* data path)]
                            (if (map? view)
                              (map #(MapEntry % navigate-request) view)
                              (map-indexed #(MapEntry [%1 %2] navigate-request) view))))))))

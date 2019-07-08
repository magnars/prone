(ns prone.ui.components.map-browser
  (:require [cljs.core.async :refer [put!]]
            [clojure.string :as str]
            [clojure.walk :as walk]
            [prone.ui.utils :refer [action get-in*]]
            [quiescent.core :as q :include-macros true]
            [quiescent.dom :as d]))

(def inline-length-limit 120)

(defn serialized-value? [v]
  (:prone.prep/original-type v))

(defn- serialized-value-shorthand [v]
  (if (serialized-value? v)
    (let [value (:prone.prep/value v)]
      (if (string? value)
        (symbol value)
        value))
    v))

(defn- serialized-value-with-type [v]
  (cond
    (and (vector? v)
         (= :prone.prep/set? (peek v)))
    (into #{} (butlast v))

    (serialized-value? v)
    (let [value (:prone.prep/value v)
          original-type (:prone.prep/original-type v)]
      (if (and (string? value)
               (str/starts-with? value "#"))
        (symbol value)
        (symbol (str value "<" original-type ">"))))

    :else v))

(defn- to-str [v]
  (pr-str (walk/prewalk serialized-value-shorthand v)))

(defn- too-long-for-inline? [v]
  (< inline-length-limit
     (.-length (pr-str (walk/prewalk serialized-value-with-type v)))))

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
  (d/span {:style {:white-space "nowrap"}}
    (InlineToken (let [v (:prone.prep/value t)]
                   (if (string? v)
                     (symbol v)
                     v)))
    (d/code {:className "subtle"} "<" (:prone.prep/original-type t) ">")))

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
  (if (= :prone.prep/set? (peek v))
    (format-list (butlast v) "#{" "}")
    (format-list v "[" "]")))

(q/defcomponent InlineListBrowser
  "Display a list in one line"
  [v navigate-request]
  (format-list v "(" ")"))

(q/defcomponent InlineSetBrowser
  "Display a set in one line"
  [v navigate-request]
  (format-list v "#{" "}"))

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
       (too-long-for-inline? m)))

(defn browseworthy-list?
  "Lists are only browseworthy if it is inconvenient to just look at the inline
  version (i.e., it is too big)"
  [t]
  (and (or (list? t) (vector? t))
       (too-long-for-inline? t)))

(q/defcomponent MapSummary
  "A map summary is a list of its keys enclosed in brackets. The summary is
   given the comment token type to visually differentiate it from fully expanded
   maps"
  [k ks navigate-request]
  (let [too-long? (too-long-for-inline? ks)
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
      (vector? v) (if (= :prone.prep/set? (peek v))
                    (d/pre {} "#{" (count v) " items}")
                    (d/pre {} "[" (count v) " items]")))))

(defn to-clipboard [text]
  (let [text-area (js/document.createElement "textarea")]
    (set! (.-textContent text-area) text)
    (js/document.body.appendChild text-area)
    (.select text-area)
    (js/document.execCommand "copy")
    (.blur text-area)
    (js/document.body.removeChild text-area)))

(q/defcomponent CopyLink [v]
  (d/td {:style {:width "20px"}
         :onClick (action #(to-clipboard (pr-str (walk/prewalk serialized-value-with-type v))))}
    "✂"))

(q/defcomponent ProneMapEntry
  "A map entry is one key/value pair, formatted appropriately for their types"
  [[k v] navigate-request]
  (d/tr {}
    (d/td {:className "name"} (InlineToken k navigate-request))
    (d/td {:style {:width "auto"}}
      (cond
        (browseworthy-map? v) (MapSummary k (keys v) navigate-request)
        (browseworthy-list? v) (ListSummary k v navigate-request)
        :else (InlineToken v navigate-request)))
    (CopyLink v)))

(q/defcomponent ProneSetEntry
  "A set entry is keyed by index, but does not show it: formats values appropriately for their types"
  [[k v] navigate-request]
  (d/tr {}
    (d/td {:style {:width "auto"}}
      (cond
        (browseworthy-map? v) (MapSummary k (keys v) navigate-request)
        (browseworthy-list? v) (ListSummary k v navigate-request)
        :else (InlineToken v navigate-request)))
    (CopyLink v)))

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
                 (cond (map? view)
                       (map #(ProneMapEntry % navigate-request) (sort-by (comp str first) view))

                       (and (vector? view) (= :prone.prep/set? (peek view)))
                       (map-indexed #(ProneSetEntry [%1 %2] navigate-request) (butlast view))

                       :else
                       (map-indexed #(ProneMapEntry [%1 %2] navigate-request) view))))))))

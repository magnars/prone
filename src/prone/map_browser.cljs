(ns prone.map-browser
  (:require [cljs.core.async :refer [put! chan <!]]
            [clojure.string :as str]
            [prone.uitilities :refer [action]]
            [quiescent :as q :include-macros true]
            [quiescent.dom :as d]))

(defn- to-str [v]
  (str/trim (prn-str v)))

(defn- get-token-class [v]
  (str "token "
       (cond
        (string? v) "string"
        (number? v) "number"
        (keyword? v) "operator")))

(q/defcomponent MapSummary [ks]
  (let [linked-keys (interpose " " (map #(d/span {:className "token comment"} (to-str %)) ks))]
    (apply d/pre {} (flatten ["{" linked-keys "}"]))))

(defn browseworthy-map?
  "Maps are only browseworthy if it is inconvenient to just look at the
   stringified version"
  [m]
  (and (map? m)
       (< 100 (.-length (to-str m)))))

(declare gen-map-entry)

(q/defcomponent ValueToken [t]
  (prn "Value" t)
  (d/code {:className (get-token-class t)} (to-str t)))

(defn prepare-inline-kv [m navigate-request]
  (let [[k v] (gen-map-entry m navigate-request)]
    ["{" k " " v "}"]))

(q/defcomponent InlineMapBrowser [m navigate-request]
  (prn "InlineMapBrowser" m)
  (let [kv-pairs (mapcat #(prepare-inline-kv % navigate-request) m)]
    (apply d/span {} kv-pairs)))

(q/defcomponent InlineToken
  "A value to be rendered roughly in one line. If the value is a list or a
   map, it will be browsable as well"
  [t navigate-request]
  (prn "InlineToken" t)
  (cond
   (map? t) (InlineMapBrowser t navigate-request)
   :else (ValueToken t)))

(defn gen-map-entry [[k v] navigate-request]
  (prn "gen-map-entry" k v)
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

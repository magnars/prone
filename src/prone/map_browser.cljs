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

(q/defcomponent NestedMap [ks]
  (let [linked-keys (interpose
                     " "
                     (map #(d/span {:className "token comment"} (to-str %)) ks))]
    (apply d/pre {} (flatten ["{" linked-keys "}"]))))

(defn gen-map-entry [[k v] navigate-request]
  (cond
   (map? v) [(d/a {:href "#"
                   :onClick (action #(put! navigate-request [:concat [k]]))} (to-str k))
             (NestedMap (keys v))]
   :else [(to-str k) (d/pre {:className (get-token-class v)} (to-str v))]))

(q/defcomponent MapEntry [m navigate-request]
  (let [[k v] (gen-map-entry m navigate-request)]
    (d/tr {}
          (d/td {:className "name"} k)
          (d/td {} v))))

(q/defcomponent MapBrowser [m navigate-request]
  (d/div {:className "inset variables"}
         (d/table {:className "var_table"}
                  (apply d/tbody {}
                         (map #(MapEntry % navigate-request) m)))))

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

(q/defcomponent MapPath [path navigate-request]
  (let [paths (map #(take (inc %) path) (range (count path)))]
    (apply d/span {}
           (flatten
            [(map #(d/a {:href "#"
                         :onClick (action (fn [] (put! navigate-request [:reset %])))}
                        (to-str (last %)))
                  (butlast paths))
             (when-let [curr (last (last paths))]
               (to-str curr))]))))

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

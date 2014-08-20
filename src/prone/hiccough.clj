(ns prone.hiccough
  "A very minimal templating DSL inspired by hiccup. Using this to avoid
   depending on a specific version of a tool that most people already have
   in their web stack"
  (:require [clojure.string :as str]))

(defn- render-attr [[k v]]
  (str " " (name k) "=\"" v "\""))

(defn tag [type attrs & contents]
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

(defn- render-map [m]
  (let [attrs (if (map? (second m)) (second m) nil)
        forms (if attrs (rest (rest m)) (rest m))]
    (tag (first m)
         (or attrs {})
         (map #(if (string? %) % (render-map %)) forms))))

(defn render-maps
  "Render one or more maps of data as markup. Translates hiccup-like
   [:div {} 'Hey'] to (tag :div {} 'Hey') and returns the resulting string"
  [& maps]
  (str/join "\n" (map render-map maps)))

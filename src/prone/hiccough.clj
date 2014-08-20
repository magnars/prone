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

(defn- render-1 [form]
  (cond
   (nil? form) ""
   (vector? form) (let [attrs (if (map? (second form)) (second form) nil)
                        forms (if attrs (rest (rest form)) (rest form))]
                    (tag (first form)
                         (or attrs {})
                         (map render-1 forms)))
   (seq? form) (str/join "\n" (map render-1 form))
   :else form))

(defn render
  "Render one or more forms of data as markup. Translates hiccup-like
   [:div {} 'Hey'] to (tag :div {} 'Hey') and returns the resulting string.
   Also accepts raw strings, numbers, nils and other data"
  [& forms]
  (str/join "\n" (map render-1 forms)))

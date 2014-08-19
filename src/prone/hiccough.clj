(ns prone.hiccough
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

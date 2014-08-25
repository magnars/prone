(ns prone.clj-code-trunc
  (:require [clojure.string :as str]))

(defn- indented? [str]
  (re-find #"^\s" str))

(defn- find-top-level-form [lines line]
  (cond
   (= 0 line)
   {:line 0 :lines lines}

   (and (not (indented? (nth lines line)))
        (not (= "" (nth lines line))))
   {:line line :lines (drop line lines)}

   :else (recur lines (dec line))))

(defn- find-first-form [lines]
  (loop [idx 1]
    (if (indented? (nth lines idx))
      (recur (inc idx))
      (str/join "\n" (take idx lines)))))

(defn truncate [code focus-line max-lines]
  (let [lines (str/split code #"\n")]
    (if (> max-lines (count lines))
      {:code code :offset 0}
      (let [top-level (find-top-level-form lines (dec focus-line))]
        {:offset (:line top-level)
         :code (find-first-form (:lines top-level))}))))

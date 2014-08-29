(ns prone.code-trunc
  "Truncate too big code samples while focusing on a certain line"
  (:require [clojure.string :as str]))

(defn- indented? [str]
  (re-find #"^\s" str))

(defn- find-top-level-form
  "Assume that every top-level form (often a function i Clojure, classes in
  Java) starts on a line with no identation. Start from the provided line, climb
  up until the closest top-level form is found. This is not a foolproof
  approach, but should provide a reasonable excerpt in many cases."
  [lines line-num]
  (cond
   (= 0 line-num)
   {:line 0 :lines lines}

   (and (not (indented? (nth lines line-num)))
        (not (= "" (nth lines line-num))))
   {:line line-num :lines (drop line-num lines)}

   :else (recur lines (dec line-num))))

(defn- find-first-form
  "Given a collection of lines, where the first one represents a top-level
  form (see find-top-level-form), return the set of lines that represents this
  whole form. Basically works by including everything until the next top-level
  form, or EOF. Again, not foolproof, but close enough for many cases."
  [lines]
  (str/join "\n" (if (= 1 (count lines))
                   lines
                   (loop [idx 1]
                     (if (and (< idx (count lines))
                              (indented? (nth lines idx)))
                       (recur (inc idx))
                       (take idx lines))))))

(defn truncate
  "Truncate code samples longer than max-lines. focus-line indicates which line
  should be at the center of the returned excerpt. truncate will attempt to
  return the relevant lines that encompasses the whole form of which focus-line
  is a part. In most cases, this means to return the function a line is found in
  for Clojure code, and the class a line is in for Java code.

  TODO: Because most Java files contain one class, it is very likely that this
  function will return an excerpt that is almost as big as the original code
  when dealing with Java code (because Java files typically only have one
  top-level form). This could be improved by detecting Java methods, and
  extracting only those instead."
  [code focus-line max-lines]
  (let [lines (str/split code #"\n")]
    (if (> max-lines (count lines))
      {:code code :offset 0}
      (let [top-level (find-top-level-form lines (dec focus-line))]
        {:offset (:line top-level)
         :code (find-first-form (:lines top-level))}))))

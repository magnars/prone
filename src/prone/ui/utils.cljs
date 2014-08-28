(ns prone.ui.utils)

(defn action [f]
  (fn [e]
    (.preventDefault e)
    (f)))

(defn update-in* [m path f]
  "Like update-in, but can map over lists by nesting paths."
  (if (vector? (last path))
    (let [nested-path (last path)
          this-path (drop-last path)]
      (if (empty? nested-path)
        (update-in m this-path (partial map f))
        (update-in m this-path (partial map #(update-in* % nested-path f)))))
    (update-in m path f)))

(defn- get-in* [data path]
  "Like get-in, but looks up indexed values in lists too."
  (loop [data data
         path path]
    (if (seq path)
      (recur (if (and (list? data)
                      (number? (first path)))
               (nth data (first path))
               (get data (first path)))
             (rest path))
      data)))

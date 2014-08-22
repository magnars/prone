(ns prone.uitilities)

(defn action [f]
  (fn [e]
    (.preventDefault e)
    (f)))

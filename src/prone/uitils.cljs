(ns prone.uitils)

(defn action [f]
  (fn [e]
    (.preventDefault e)
    (f)))

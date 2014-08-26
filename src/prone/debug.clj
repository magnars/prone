(ns prone.debug)

(def debug-data (atom []))

(defn queue-debug [{:keys [forms message] :as data}]
  (swap! debug-data conj (merge data {:message (if (string? message) message nil)
                                      :forms (if (string? message) forms (concat [message] forms))}))
  nil)

(defmacro debug [message & forms]
  (list 'prone.debug/queue-debug {:line (:line (meta &form))
                      :column (:column (meta &form))
                      :file *file*
                      :locals (into {} (map (fn [l] [`'~l l]) (reverse (keys &env))))
                      :message message
                      :forms (list 'quote forms)}))

(ns prone.debug)

(def debug-data (atom []))

(defn queue-debug [{:keys [forms message] :as data}]
  (swap! debug-data conj (merge data {:id (count @debug-data)
                                      :message (if (string? message) message nil)
                                      :forms (if (string? message) forms (concat [message] forms))}))
  nil)

(defmacro debug [message & forms]
  (list 'prone.debug/queue-debug {:line-number (:line (meta &form))
                      :column (:column (meta &form))
                      :file-name *file*
                      :locals (into {} (map (fn [l] [`'~l l]) (reverse (keys &env))))
                      :message message
                      :forms (list 'quote forms)}))

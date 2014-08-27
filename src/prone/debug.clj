(ns prone.debug)

(def debug-data (atom []))

(defn queue-debug [{:keys [forms message locals class-path-url] :as data}]
  (swap! debug-data conj (merge data {:id (count @debug-data)
                                      :message (if (string? message) message nil)
                                      :forms (if (string? message) forms (concat [message] forms))
                                      :locals (if (seq locals) locals nil)}))
  nil)

(defmacro debug [message & forms]
  (list 'prone.debug/queue-debug {:line-number (:line (meta &form))
                      :column (:column (meta &form))
                      :class-path-url *file*
                      :locals (into {} (map (fn [l] [`'~l l]) (reverse (keys &env))))
                      :message message
                      :forms (list 'quote forms)}))

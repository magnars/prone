(ns prone.debug)

(def ^:dynamic *debug-data*)

(defn queue-debug [{:keys [forms locals class-path-url] :as data}]
  (swap! *debug-data*
         (fn [debug-data]
           (conj debug-data
                 (merge data {:id (count debug-data)
                              :locals (if (seq locals) locals nil)
                              :forms (if (seq forms) forms nil)}))))
  nil)

(defmacro debug [& forms]
  (let [message (when (string? (first forms)) (first forms))
        forms (if (string? (first forms)) (rest forms) forms)]
    (list 'prone.debug/queue-debug {:line-number (:line (meta &form))
                                    :column (:column (meta &form))
                                    :class-path-url *file*
                                    :locals (into {} (map (fn [l] [`'~l l]) (reverse (keys &env))))
                                    :message message
                                    :forms (vec forms)})))

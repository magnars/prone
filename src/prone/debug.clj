(ns prone.debug)

(def ^:dynamic *debug-data*
  "Debug data is stored in a dynamic var in order to be thread-safe.
   The var must be bound to an atom containing a vector before using the `debug`
   macro. The prone middleware sets up this binding locally for each request.")

(defn queue-debug [{:keys [forms locals class-path-url] :as data}]
  (swap! *debug-data*
         (fn [debug-data]
           (conj debug-data
                 (merge data {:id (count debug-data)
                              :locals (if (seq locals) locals nil)
                              :forms (if (seq forms) forms nil)}))))
  nil)

(defmacro debug
  "Debug application with Prone. Queues up a source location along with
   available local bindings, and provided message/data. Note that the macro
   always returns nil. The macro is only used for side-effects, queueing up
   messages to render when the request handler reaches the Prone middleware."
  [& forms]
  (let [message (when (string? (first forms)) (first forms))
        forms (if (string? (first forms)) (rest forms) forms)]
    (list 'prone.debug/queue-debug {:line-number (:line (meta &form))
                                    :column (:column (meta &form))
                                    :class-path-url *file*
                                    :locals (into {} (map (fn [l] [`'~l l]) (reverse (keys &env))))
                                    :message message
                                    :forms (vec forms)})))

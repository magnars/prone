(ns prone.ui.main
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :refer [chan <!]]
            [cljs.reader :as reader]
            [prone.ui.components.app :refer [ProneUI]]
            [prone.ui.utils :refer [update-in*]]
            [quiescent :as q :include-macros true]))

(defn update-selected-frame [data frame-id]
  (let [path (concat [:error] (-> data :paths :error) [:frames []])]
    (update-in* data path
                #(if (= (:id %) frame-id)
                   (assoc % :selected? true)
                   (dissoc % :selected?)))))

(defn navigate-map [name data navigation]
  (case (first navigation)
    :concat (update-in data [:paths name] #(concat % (second navigation)))
    :reset (assoc-in data [:paths name] (second navigation))))

(defn code-excerpt-changed? [old new]
  (not (and (= (:error new) (:error old))
            (= (-> new :paths :error) (-> old :paths :error)))))

(defn prepare-data-for-display [data]
  (update-in data [:error] #(get-in % (-> data :paths :error))))

(defn handle-data-change [chans key ref old new]
  (q/render (ProneUI (prepare-data-for-display new) chans)
            (.getElementById js/document "ui-root"))
  (when (code-excerpt-changed? old new)
    (.highlightAll js/Prism)
    (set! (-> js/document (.getElementById "frame-info") .-scrollTop) 0)))

(defn on-msg [chan handler]
  (go-loop []
    (when-let [msg (<! chan)]
      (handler msg)
      (recur))))

(defn bootstrap! [data]
  (let [chans {:select-frame (chan)
               :change-frame-filter (chan)
               :navigate-request (chan)
               :navigate-data (chan)
               :navigate-error (chan)}
        prone-data (atom nil)]

    (on-msg (:select-frame chans) #(swap! prone-data update-selected-frame %))
    (on-msg (:change-frame-filter chans) #(swap! prone-data assoc :frame-filter %))
    (on-msg (:navigate-request chans) #(swap! prone-data (partial navigate-map :request) %))
    (on-msg (:navigate-data chans) #(swap! prone-data (partial navigate-map :data) %))
    (on-msg (:navigate-error chans) #(swap! prone-data (partial navigate-map :error) %))

    (add-watch prone-data :state-change (partial handle-data-change chans))
    (reset! prone-data data)))

(bootstrap! (-> js/document
                (.getElementById "prone-data")
                .-innerHTML
                reader/read-string))

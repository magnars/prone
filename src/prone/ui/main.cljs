(ns prone.ui.main
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :refer [chan <!]]
            [cljs.reader :as reader]
            [clojure.string :as str]
            [prone.ui.components.app :refer [ProneUI]]
            [prone.ui.utils :refer [update-in*]]
            [quiescent :as q :include-macros true]))

(defn update-selected-frame [data frame-id]
  (let [path (concat [:error] (-> data :paths :error) [:frames []])]
    (update-in* data path
                #(if (= (:id %) frame-id)
                   (assoc % :selected? true)
                   (dissoc % :selected?)))))

(defn update-selected-debug [data id]
  (update-in* data [:debug-data []]
              #(if (= (:id %) id)
                 (assoc % :selected? true)
                 (dissoc % :selected?))))

(defn update-selected-src-loc [data {:keys [id type]}]
  (case type
    :frame (swap! data update-selected-frame id)
    :debug (swap! data update-selected-debug id)))

(defn code-excerpt-changed? [old new]
  (not (and (= (:error new) (:error old))
            (= (-> new :paths :error) (-> old :paths :error))
            (= (filter :selected? (-> new :debug-data))
               (filter :selected? (-> old :debug-data))))))

(defn prepare-data-for-display [data]
  (update-in data [:error] #(get-in % (-> data :paths :error))))

(defn handle-data-change [chans key ref old new]
  (q/render (ProneUI (prepare-data-for-display new) chans)
            (.getElementById js/document "ui-root"))
  (when (code-excerpt-changed? old new)
    (.highlightAll js/Prism)
    (set! (-> js/document (.getElementById "frame-info") .-scrollTop) 0)))

(defn navigate-data [data [path-key [nav-type path]]]
  (case nav-type
    :concat (update-in data [:paths path-key] #(concat % path))
    :reset (assoc-in data [:paths path-key] path)))

(defn on-msg [chan handler]
  (go-loop []
    (when-let [msg (<! chan)]
      (handler msg)
      (recur))))

(defn bootstrap! [data]
  (let [chans {:select-frame (chan)
               :change-frame-selection (chan)
               :navigate-request (chan)
               :navigate-data (chan)
               :navigate-error (chan)}
        prone-data (atom nil)]

    (on-msg (:select-frame chans) #(update-selected-src-loc prone-data %))
    (on-msg (:change-frame-selection chans) #(swap! prone-data assoc :frame-selection %))
    (on-msg (:navigate-data chans) #(swap! prone-data navigate-data %))

    (add-watch prone-data :state-change (partial handle-data-change chans))
    (reset! prone-data data)))

(defn unescape-script-tags [s]
  "Script tags must be escaped on the server, or the browser is properly confused.
   Get the server-mandated replacement string, and put those script tags back in."
  (let [script-replacement-string (-> js/document (.getElementById "script-replacement-string") .-value)]
    (str/replace s script-replacement-string "script")))

(bootstrap! (-> js/document
                (.getElementById "prone-data")
                .-innerHTML
                unescape-script-tags
                reader/read-string))

(ns prone.ui.main
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :refer [chan <!]]
            [cljs.reader :as reader]
            [clojure.string :as str]
            [prone.ui.components.app :refer [ProneUI]]
            [prone.ui.utils :refer [update-in*]]
            [quiescent :as q :include-macros true]))

(defn code-excerpt-changed? [old new]
  (or (not= (:selected-src-locs new) (:selected-src-locs old))
      (not= (:src-loc-selection new) (:src-loc-selection old))))

(defn get-active-src-locs [{:keys [error src-loc-selection debug-data]}]
  (case src-loc-selection
    :all (:frames error)
    :application (filter :application? (:frames error))
    :debug debug-data))

(defn select-current-error-in-chain [data]
  (update-in data [:error] #(get-in % (-> data :paths :error))))

(defn prepare-data-for-display [data]
  (let [data (select-current-error-in-chain data)]
    (-> data
        (assoc :active-src-locs (get-active-src-locs data))
        (assoc :selected-src-loc (get-in data [:selected-src-locs (:src-loc-selection data)])))))

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

(defn select-src-loc [data selection]
  (assoc-in data [:selected-src-locs (:src-loc-selection data)] selection))

(defn ensure-selected-src-loc [data]
  (if (get-in data [:selected-src-locs (:src-loc-selection data)])
    data
    (select-src-loc data (first (get-active-src-locs data)))))

(defn change-src-loc-selection [data selection]
  (-> data
      (assoc :src-loc-selection selection)
      ensure-selected-src-loc))

(defn initialize-data [data]
  (-> data
      ensure-selected-src-loc))

(defn bootstrap! [data]
  (let [chans {:change-src-loc-selection (chan)
               :select-src-loc (chan)
               :navigate-request (chan)
               :navigate-data (chan)
               :navigate-error (chan)}
        prone-data (atom nil)]

    (on-msg (:select-src-loc chans) #(swap! prone-data select-src-loc %))
    (on-msg (:change-src-loc-selection chans) #(swap! prone-data change-src-loc-selection %))
    (on-msg (:navigate-data chans) #(swap! prone-data navigate-data %))

    (add-watch prone-data :state-change (partial handle-data-change chans))
    (reset! prone-data (initialize-data data))))

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

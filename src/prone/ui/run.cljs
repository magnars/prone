(ns prone.ui.run
  "Start the fun!"
  (:require [prone.ui.main :refer [bootstrap! unescape-script-tags]]))

(bootstrap! (-> js/document
                (.getElementById "prone-data")
                .-innerHTML
                unescape-script-tags
                reader/read-string))

(defproject prone "0.1.0"
  :description "Better exception reporting middleware for Ring."
  :dependencies [[org.clojure/clojure "1.5.1"]]
  :profiles {:dev {:dependencies [[flare "0.1.1"]
                                  [ring "1.2.1"]
                                  [hiccup-find  "0.4.0"]]
                   :injections [(require 'flare.clojure-test)
                                (flare.clojure-test/install!)]
                   :source-paths ["dev"]
                   :ring {:handler prone.demo/app}
                   :plugins [[lein-ring "0.8.10"]]}})

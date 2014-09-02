(defproject prone "0.7.0-SNAPSHOT"
  :description "Better exception reporting middleware for Ring."
  :url "http://github.com/magnars/prone"
  :license {:name "GNU General Public License v3"
            :url "http://www.gnu.org/licenses/"}
  :dependencies [[org.clojure/clojure "1.5.1"]]
  :plugins [[lein-shell "0.3.0"]]
  :prep-tasks [["shell" "./build-js-sources.sh"]]
  :profiles {:dev {:dependencies [[flare "0.1.1"]
                                  [ring "1.2.1"]
                                  [hiccup-find  "0.4.0"]
                                  [org.clojure/clojurescript "0.0-2202"]
                                  [prismatic/schema "0.2.1"]
                                  [quiescent "0.1.4"]
                                  [org.clojure/core.async "0.1.338.0-5c5012-alpha"]]
                   :injections [(require 'flare.clojure-test)
                                (flare.clojure-test/install!)]
                   :source-paths ["dev"]
                   :ring {:handler prone.demo/app}
                   :plugins [[lein-ring "0.8.10"]
                             [lein-cljsbuild "1.0.3"]]
                   :cljsbuild {:builds [{:source-paths ["src" "dev"]
                                         :compiler {:output-to "resources/prone/generated/prone.js"
                                                    :optimizations :whitespace
                                                    :output-dir "resources/prone/generated/out"
                                                    ;:source-map "resources/prone/generated/prone.js.map"
                                                    }}]}}})

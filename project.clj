(defproject prone "1.5.2"
  :description "Better exception reporting middleware for Ring."
  :url "http://github.com/magnars/prone"
  :license {:name "BSD-3-Clause"
            :url "http://opensource.org/licenses/BSD-3-Clause"}
  :dependencies [[realize "1.1.0"]]
  :filespecs [{:type :fn
               :fn (fn [_]
                     {:type :bytes :path "prone/generated/prone.js"
                      :bytes (slurp "dev-resources/prone/generated/prone.js")})}]
  :profiles {:dev {:dependencies [[org.clojure/clojure "1.9.0"]
                                  [flare "0.2.9"]
                                  [ring "1.6.3"]
                                  [hiccup-find  "0.5.0"]
                                  [org.clojure/clojurescript "1.9.946"]
                                  [quiescent "0.3.2"]
                                  [org.clojure/core.async "0.4.474"]
                                  [com.datomic/datomic-free "0.9.5544" :exclusions [joda-time commons-codec com.google.guava/guava]]]
                   :injections [(require 'flare.clojure-test)
                                (flare.clojure-test/install!)]
                   :source-paths ["dev"]
                   :prep-tasks [["shell" "./build-js-sources.sh"]]
                   :ring {:handler prone.demo/app
                          :port 3001}
                   :plugins [[lein-ring "0.12.3"]
                             [lein-cljsbuild "1.1.7"]
                             [lein-shell "0.5.0"]
                             [com.jakemccrary/lein-test-refresh "0.22.0"]]
                   :cljsbuild {:builds [{:source-paths ["src" "dev"]
                                         :compiler {:output-to "dev-resources/prone/generated/prone.js"
                                                    :output-dir "dev-resources/prone/generated/out"
                                                    :optimizations :whitespace}}]}}})

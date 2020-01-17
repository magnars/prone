(defproject prone "2020-01-17"
  :description "Better exception reporting middleware for Ring."
  :url "http://github.com/magnars/prone"
  :license {:name "BSD-3-Clause"
            :url "http://opensource.org/licenses/BSD-3-Clause"}
  :dependencies [[realize "2019-04-24"]]
  :filespecs [{:type :fn
               :fn (fn [_]
                     {:type :bytes :path "prone/generated/prone.js"
                      :bytes (slurp "dev-resources/prone/generated/prone.js")})}]
  :profiles {:dev {:dependencies [[org.clojure/clojure "1.9.0"]
                                  [ring "1.6.3"]
                                  [ring-server "0.5.0"]
                                  [hiccup-find  "0.5.0"]
                                  [org.clojure/clojurescript "1.9.946"]
                                  [quiescent "0.3.2"]
                                  [org.clojure/core.async "0.4.474"]
                                  [com.datomic/datomic-free "0.9.5544" :exclusions [joda-time commons-codec com.google.guava/guava org.clojure/tools.cli]]]
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
                                                    :optimizations :whitespace}}]}}
             :kaocha {:dependencies [[lambdaisland/kaocha "0.0-418"]]}}
  :aliases {"kaocha" ["with-profile" "+kaocha" "run" "-m" "kaocha.runner"]})

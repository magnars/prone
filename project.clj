(defproject prone "0.8.1"
  :description "Better exception reporting middleware for Ring."
  :url "http://github.com/magnars/prone"
  :license {:name "GNU General Public License v3"
            :url "http://www.gnu.org/licenses/"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2913"]
                 [cljsjs/react "0.12.2-7"]
                 [quiescent "0.1.4"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]]
  :plugins [[lein-shell "0.3.0"]
            [lein-cljsbuild "1.0.5"]
            [lein-asset-minifier "0.2.2"]
            [com.jakemccrary/lein-test-refresh "0.5.5"]]
  :prep-tasks [["shell" "./build-js-sources.sh"]]

  :release-tasks [["shell" "./build-js-sources.sh"]
                  ["minify-assets"]
                  ["with-profiles" "-dev" ["cljsbuild" "once"]]
                  ["jar"]]

  :aliases {"build-auto" ["pdo" ["minify-assets" "watch"] ["cljsbuild" "auto"]]
            "build"      ["do" ["minify-assets"] ["cljsbuild" "once"]]}

  :minify-assets {:assets  {"resources/css/prone.css" ["dev-resources/prone/css/better-errors.css"
                                                              "dev-resources/prismjs/themes/prism.css"
                                                              "dev-resources/prismjs/plugins/line-highlight/prism-line-highlight.css"
                                                              "dev-resources/prismjs/plugins/line-numbers/prism-line-numbers.css"
                                                              "dev-resources/prone/css/styles.css"]

                            "resources/js/support.js" ["dev-resources/prismjs/prism.js"
                                                              "dev-resources/prismjs/plugins/line-numbers/prism-line-numbers.min.js"
                                                              "dev-resources/prismjs/plugins/line-highlight/prism-line-highlight.min.js"
                                                              "dev-resources/prone/vendor/prism-line-numbers.js"
                                                              "dev-resources/prism-clojure/prism.clojure.js"]}
                  :options {:optimizations :whitespace}}

  :cljsbuild {:builds {:prone {:source-paths ["src"]
                               :compiler     {:output-to     "resources/js/prone.js"
                                              ;:source-map "resources/js/prone.js.map"
                                              :optimizations :whitespace}}}}

  :profiles {:dev {:dependencies [[flare "0.2.2"]
                                  [ring "1.2.1"]
                                  [hiccup-find "0.4.0"]
                                  [prismatic/schema "0.2.1"]
                                  [lein-pdo "0.1.1"]]
                   :injections   [(require 'flare.clojure-test)
                                  (flare.clojure-test/install!)]
                   :source-paths ["dev"]
                   :ring         {:handler prone.demo/app}
                   :plugins      [[lein-ring "0.8.10"]]
                   :cljsbuild    {:builds {:prone {:source-paths ["dev"]}}}}})

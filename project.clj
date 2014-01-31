(defproject ringi "0.1.0-SNAPSHOT"
  :description "FIXME: write this!"
  :url "http://ringi.co/"

  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/tools.reader "0.8.2"]
                 ;; CLJ
                 [ring/ring-core "1.2.0"]
                 [compojure "1.1.6"]
                 [cheshire "5.2.0"]
                 ;; CLJS
                 [org.clojure/clojurescript "0.0-2138"]
                 [org.clojure/core.async "0.1.267.0-0d7780-alpha"]
                 [cljs-http "0.1.5"]
                 [om "0.3.0"]]

  :plugins [[lein-cljsbuild "1.0.1"]
            [lein-ring "0.8.7"]
            [lein-pdo "0.1.1"]]

  :aliases {"dev" ["pdo" "cljsbuild" "auto" "dev," "ring" "server-headless"]}

  :ring {:handler ringi.core/app
         :init    ringi.core/init}

  :source-paths ["src/clj"]

  :cljsbuild {
              :builds [{:id "dev"
                        :source-paths ["src/cljs"]
                        :compiler {
                                   :output-to "resources/public/js/ringi.js"
                                   :output-dir "resources/public/js/out"
                                   :optimizations :none
                                   :source-map true
                                   :externs ["react/externs/react.js"]}}
                       {:id "release"
                        :source-paths ["src/cljs"]
                        :compiler {:output-dir "resources/public/js"
                                   :output-to "resources/public/js/ringi.js"
                                   :source-map "resources/public/js/ringi.js.map"
                                   :optimizations :advanced
                                   :pretty-print false
                                   :output-wrapper false
                                   :preamble ["react/react.min.js"]
                                   :externs ["react/externs/react.js"]
                                   :closure-warnings
                                   {:non-standard-jsdoc :off}}}]})

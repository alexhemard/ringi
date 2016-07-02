(defproject ringi "1.0.0-SNAPSHOT"
  :description "consensus app"
  :url "http://ringi.co/"

  :dependencies [[ring "1.4.0"]
                 [amalloy/ring-gzip-middleware "0.1.3"]
                 [bidi "2.0.9"]
                 [bouncer "1.0.0"]
                 [cheshire "5.6.3"]
                 [clj-http "2.1.0"]
                 [clj-oauth "1.5.4"]
                 [com.cemerick/friend "0.2.3"]
                 [com.cemerick/url "0.1.1"]
                 [com.cognitect/transit-clj "0.8.285"]
                 [com.cognitect/transit-cljs "0.8.239"]
                 [com.datomic/datomic-pro "0.9.5385" :exclusions [joda-time]]
                 [com.stuartsierra/component "0.3.1"]
                 [commons-codec "1.10"]
                 [crypto-password "0.1.3"]
                 [environ "1.0.1"]
                 [hiccup "1.0.5"]
                 [org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.89"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.clojure/tools.reader "1.0.0-beta3"]
                 [org.immutant/web "2.1.5" :exclusions [ch.qos.logback/logback-classic]]
                 [org.omcljs/om "1.0.0-alpha37" :exclusions [com.cognitect/transit-cljs]]
                 [slingshot "0.12.2"]]

  :repositories {"my.datomic.com" {:url "https://my.datomic.com/repo"
                                   :username :env/datomic_user
                                   :password :env/datomic_key }}

  :source-paths ["src/clj" "src/cljc"]

  :repl-options {:init-ns dev}
  
  :profiles {:dev [:dev-common]
             :dev-common {:repositories {"my.datomic.com" {:url "~/.m2"}}
                   :source-paths ["dev"]
                   :dependencies [[org.clojure/tools.namespace "0.2.11"]
                                  [figwheel-sidecar "0.5.0-SNAPSHOT"]
                                  [binaryage/devtools "0.4.1"]
                                  [org.clojure/tools.nrepl "0.2.11"]]}
             :uberjar {:aot :all
                       :prep-tasks ^:replace ["npm-install"
                                              ["cljsbuild" "once" "production"]
                                              "gulp"
                                              "javac"
                                              "compile"]}}

  :plugins [[lein-cljsbuild "1.1.2"]
            [lein-figwheel "0.5.0-6"]
            [lein-environ "1.0.2"]]

  :aliases {"dev"         ["cljsbuild" "auto" "dev," "ring" "server-headless"]
            "migrate"     ["run" "-m" "ringi.db.migrate"]
            "gulp"        ["shell" "node_modules/gulp/bin/gulp.js" "build"]
            "npm-install" ["shell" "npm" "install"]}

  :test-paths ["test"]

  :main ringi.core

  :min-lein-version "2.0.0"

  :cljsbuild {
    :builds [{:id "dev"
              :source-paths ["src/cljs" "src/cljc" "dev"]
              :compiler {:main ringi.core
                         :output-to "resources/public/js/ringi.js"
                         :source-map true
                         :output-dir "resources/public/js/out"
                         :source-map-path "js/out"
                         :asset-path "/js/out"
                         :optimizations :none
                         :figwheel true
                         :warnings {:single-segment-namespace false}}}
             {:id "release"
              :source-paths ["src/cljs"]
              :compiler {:output-dir "resources/public/js"
                         :output-to "resources/public/js/ringi.js"
                         :asset-path "/"
                         :main ringi.core
                         :optimizations :advanced
                         :warnings {:single-segment-namespace false}
                         :pretty-print false}}]})

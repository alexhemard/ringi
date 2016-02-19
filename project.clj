(defproject ringi "0.1.0-SNAPSHOT"
  :description "FIXME: write this!"
  :url "http://ringi.co/"

  :dependencies [[bidi "1.25.1"]
                 [bouncer "1.0.0"]
                 [clj-http "2.1.0"]
                 [clj-oauth "1.5.4"]
                 [com.cemerick/url "0.1.1"]
                 [com.cognitect/transit-clj "0.8.285"]
                 [com.cognitect/transit-cljs "0.8.225"]
                 [com.datomic/datomic-pro "0.9.5350" :exclusions [joda-time]]
                 [com.stuartsierra/component "0.3.1"]
                 [commons-codec "1.10"]
                 [crypto-password "0.1.3"]
                 [datascript "0.15.0"]
                 [environ "1.0.1"]
                 [hiccup "1.0.5"]
                 [org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.7.228"]
                 [org.clojure/core.async "0.2.374"]
                 [org.clojure/tools.cli "0.3.3"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.clojure/tools.nrepl "0.2.11"]
                 [org.clojure/tools.reader "1.0.0-alpha3"]
                 [org.immutant/web "2.1.2" :exclusions [ch.qos.logback/logback-classic]]
                 [org.omcljs/om "0.9.0"]
                 [org.omcljs/om "1.0.0-alpha30-SNAPSHOT" :exclusions [com.cognitect/transit-cljs]]
                 [ring "1.4.0"]
                 [slingshot "0.12.2"]]

  :repositories {"my.datomic.com" {:url "https://my.datomic.com/repo"
                                   :username :env/datomic_user
                                   :password :env/datomic_key }}

  :source-paths ["src/clj" "src/cljs" "src/cljc"]

  :profiles {:dev [:dev-common]
             :dev-common {:repositories {"my.datomic.com" {:url "~/.m2"}}
                   :source-paths ["dev"]
                   :dependencies [[org.clojure/tools.namespace "0.2.11"]
                                  [figwheel-sidecar "0.5.0-SNAPSHOT"]
                                  [binaryage/devtools "0.4.1"]
                                  [org.clojure/tools.nrepl "0.2.11"]]}
             :uberjar {:aot :all}}

  :plugins [[lein-cljsbuild "1.1.2"]
            [lein-figwheel "0.5.0-6"]
            [lein-environ "1.0.2"]
            [cider/cider-nrepl "0.10.2"]]

  :aliases {"dev"     ["cljsbuild" "auto" "dev," "ring" "server-headless"]
            "migrate" ["run" "-m" "ringi.db.migrate"]}

  :test-paths ["test"]

  :main ringi.system

  :min-lein-version "2.0.0"

  :cljsbuild {
    :builds [{:id "dev"
              :source-paths ["src/cljs" "src/cljc" "dev"]
              :compiler {:main ringi.core
                         :output-to "resources/public/js/ringi.js"
                         :source-map "resources/public/js/ringi.js.map"
                         :output-dir "resources/public/js/out"
                         :source-map-path "js/out"
                         :asset-path "/js/out"
                         :optimizations :none
                         :warnings {:single-segment-namespace false}}}
             {:id "release"
              :source-paths ["src/cljs"]
              :compiler {:output-dir "resources/public/js"
                         :output-to "resources/public/js/ringi.js"
                         :asset-path "/"
                         :main ringi.core
                         :optimizations :advanced
                         :pretty-print false}}]})

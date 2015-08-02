(defproject ringi "0.1.0-SNAPSHOT"
  :description "FIXME: write this!"
  :url "http://ringi.co/"

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/tools.reader "0.9.2"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.clojure/tools.cli "0.3.1"]
                 [org.clojure/tools.nrepl "0.2.10"]

                 ;; CLJ

                 [cheshire "5.4.0"]
                 [clj-http "2.0.0"]
                 [clj-oauth "1.5.2"]
                 [commons-codec "1.10"]
                 [com.stuartsierra/component "0.2.3"]
                 [compojure "1.4.0"]
                 [crypto-password "0.1.3"]
                 [com.datomic/datomic-pro "0.9.5201" :exclusions [joda-time]]
                 [ring "1.4.0"]
                 [aleph "0.4.0"]
                 [hiccup "1.0.5"]
                 [slingshot "0.12.2"]
                 [jkkramer/verily "0.6.0"]

                 ;; CLJS

                 [org.clojure/clojurescript "0.0-3308"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [datascript "0.11.6"]
                 [cljs-http "0.1.35"]
                 [secretary "1.2.1"]
                 [reagent "0.5.0" :exclusions [cljsjs/react]]
                 [cljsjs/react-with-addons "0.13.3-0"]]

  :repositories {"my.datomic.com" {:url "~/.m2"}}
  :profiles {:test {:source-paths ["test"]}
             :dev {:source-paths ["dev"]
                   :dependencies [[org.clojure/tools.namespace "0.2.10"]
                                  [org.clojure/java.classpath "0.2.2"]]}}

  :plugins [[lein-cljsbuild "1.0.6"]
            [lein-ring "0.9.6"]
            [cider/cider-nrepl "0.9.1"]
            [lein-figwheel "0.3.7"]
            [quickie "0.4.0"]]

  :aliases {"dev"     ["cljsbuild" "auto" "dev," "ring" "server-headless"]
            "migrate" ["run" "-m" "ringi.db.migrate"]}

  :test-paths ["test"]

  :main ringi.system

  :figwheel {:nrepl-port 7002}

  :source-paths ["src/clj"]

  :cljsbuild {
    :builds [{:id "dev"
              :source-paths ["src/cljs"]
              :compiler {:main ringi.core
                         :output-to "resources/public/js/ringi.js"
                         :source-map "resources/public/js/ringi.js.map"
                         :output-dir "resources/public/js/out"
                         :source-map-path "js/out"
                         :asset-path "/js/out"
                         :optimizations :none}}
             {:id "release"
              :source-paths ["src/cljs"]
              :compiler {:output-dir "resources/public/js"
                         :output-to "ringi.js"
                         :asset-path "/"
                         :main ringi.core
                         :optimizations :advanced
                         :pretty-print false}}]})

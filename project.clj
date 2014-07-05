(defproject ringi "0.1.0-SNAPSHOT"
  :description "FIXME: write this!"
  :url "http://ringi.co/"

  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/tools.reader "0.8.2"]
                 [org.clojure/tools.logging "0.2.6"]
                 [org.clojure/tools.cli "0.2.4"]
                 ;; CLJ
                 [bultitude "0.2.2"]
                 [cheshire "5.2.0"]
                 [clj-oauth "1.4.1"]
                 [commons-codec "1.9"]
                 [com.jolbox/bonecp "0.8.0.RELEASE"]
                 [com.stuartsierra/component "0.2.0"]
                 [compojure "1.1.6"]
                 [crypto-password "0.1.1"]
                 [http-kit "2.1.10"]
                 [honeysql "0.4.3"]
                 [org.clojure/java.jdbc "0.3.3"]
                 [org.postgresql/postgresql "9.2-1003-jdbc4"]
                 [ring/ring-core "1.2.0"]
                 [ring/ring-devel "1.2.0"]
                 [slingshot "0.10.3"]
                 [jkkramer/verily "0.6.0"]
                 ;; CLJS
                 [org.clojure/clojurescript "0.0-2173"]
                 [org.clojure/core.async "0.1.278.0-76b25b-alpha"]
                 [cljs-http "0.1.8"]
                 [secretary "1.1.0"]
                 [om "0.5.3"]]

  :profiles {:dev {:source-paths ["dev"]
                   :plugins      [[com.aphyr/prism "0.1.1"]]
                   :dependencies [[org.clojure/tools.namespace "0.2.4"]
                                  [org.clojure/java.classpath "0.2.2"]
                                  [com.aphyr/prism "0.1.1"]]}}

  :plugins [[lein-cljsbuild "1.0.2"]
            [lein-ring "0.8.7"]
            [lein-pdo "0.1.1"]]

  :aliases {"dev"     ["pdo" "cljsbuild" "auto" "dev," "ring" "server-headless"]
            "migrate" ["run" "-m" "ringi.db.migrate"]}

  :test-paths ["test"]

  :main ringi.system

  :source-paths ["src/clj"]

  :cljsbuild {
    :builds [{:id "dev"
              :source-paths ["src/cljs"]
              :compiler {:output-to "resources/public/js/ringi.js"
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

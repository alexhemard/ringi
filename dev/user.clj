(ns user
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.pprint :refer [pprint]]
            [clojure.repl :refer :all]
            [clojure.test :as test]
            [ringi.system]
            [ringi.config]
            [com.stuartsierra.component :as component]
            [clojure.tools.namespace.repl :refer (refresh refresh-all)]))

(def system nil)

(defn init
  "Constructs the current development system."
  []
  (alter-var-root #'system
                  (constantly (ringi.system/system (ringi.config/config)))))

(defn start
  "Starts the current development system."
  []
  (alter-var-root #'system  component/start-system))

(defn stop
  "Shuts down and destroys the current development system."
  []
  (alter-var-root #'system component/stop-system))

(defn go
  "Initializes the current development system and starts it running."
  []
  (init)
  (start))

(defn reset []
  (stop)
  (refresh :after 'user/go))


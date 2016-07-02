(ns ringi.core
  (:require [com.stuartsierra.component :as component]
            [ringi.system :refer [system]]
            [ringi.util   :refer [parse-int]]            
            [environ.core :refer [env]])
  (:gen-class))

(def default-config
  {:env (env :ringi-env "development")
   :http {:port (parse-int (env :port 5000))
          :host "0.0.0.0"}
   :datomic {:uri (env :datomic-url "datomic:mem://ringi")}})

(defn -main []
  (component/start-system
   (system default-config)))

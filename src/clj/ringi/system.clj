(ns ringi.system
  (:require [com.stuartsierra.component :as     component]
            [ringi.datomic              :refer [create-datomic]]
            [ringi.app                  :refer [create-app]]
            [ringi.http                 :refer [create-http-server]]
            [ringi.config               :as     conf]))

(def system-components [:datomic :app :http])

(defrecord RingiSystem [config datomic app http]
  component/Lifecycle
  (start [this]
    (component/start-system this system-components))
  (stop [this]
    (component/stop-system this system-components)))

(defn system [config]
  (let [datomic-config    (:datomic config)
        http-config  (:http config)]
    (map->RingiSystem
      {:config  config
       :datomic (create-datomic datomic-config)
       :app (component/using
                 (create-app)
                 {:datomic  :datomic})
       :http    (component/using
                 (create-http-server http-config)
                 {:app :app})})))

(defn -main [& args]
  (let [config (conf/config)]
    (.start (system config))))

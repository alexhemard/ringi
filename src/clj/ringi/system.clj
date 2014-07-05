(ns ringi.system
  (:require [com.stuartsierra.component :as     component]
            [ringi.db               :refer [create-database]]
            [ringi.server           :refer [create-http-server]]
            [ringi.app              :refer [create-app]]
            [ringi.config           :as    conf]))

(def system-components [:db :app :http])

(defrecord RingiSystem [config db app http]
  component/Lifecycle
  (start [this]
    (component/start-system this system-components))
  (stop [this]
    (component/stop-system this system-components)))


(defn system [config]
  (let [db-config    (:db config)
        http-config  (:http config)]
    (map->RingiSystem
      {:config  config
       :db      (create-database db-config)
       :app     (component/using
                 (create-app)
                 {:db    :db})
       :http    (component/using
                 (create-http-server http-config)
                 {:app :app})})))

(defn -main [& args]
  (let [config (conf/config)]
    (.start (system config))))

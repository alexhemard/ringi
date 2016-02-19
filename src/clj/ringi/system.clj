(ns ringi.system
  (:require [com.stuartsierra.component :as     component]
            [immutant.util              :refer [set-log-level!]]
            [ringi.datomic              :refer [create-datomic]]
            [ringi.app                  :refer [create-app]]
            [ringi.http                 :refer [create-http-server]]
            [ringi.tx-listener          :refer [create-tx-listener]]
            [ringi.config               :as     conf])
  (:gen-class))

(defn system [config]
  (let [datomic-config    (:datomic config)
        http-config  (:http config)]
    (-> (component/system-map
         :config       config
         :datomic     (create-datomic datomic-config)
         :tx-listener (create-tx-listener)
         :app         (create-app)
         :http        (jetty-server http-config))
        (component/system-using
          {:app         [:datomic]
           :http        [:app]}))))

(ns ringi.system
  (:require [com.stuartsierra.component :as     component]
            [immutant.util              :refer [set-log-level!]]
            [ringi.datomic              :refer [create-datomic]]
            [ringi.app                  :refer [create-app]]
            [ringi.http                 :refer [create-http-server]]
            [ringi.config               :as     conf]))

(defn system [config]
  (let [datomic-config (:datomic config)
        http-config    (:http config)]
    (-> (component/system-map
         :config       config
         :datomic     (create-datomic datomic-config)
         :app         (create-app)
         :http        (create-http-server http-config))
        (component/system-using
         {:app         [:datomic]
          :http        [:app]}))))

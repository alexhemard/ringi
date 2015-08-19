(ns ringi.system
  (:require [com.stuartsierra.component :as     component]
            [ringi.datomic              :refer [create-datomic]]
            [ringi.app                  :refer [create-app]]
            [ringi.http                 :refer [create-http-server]]
            [ringi.config               :as     conf])
  (:gen-class))

(defn system [config]
  (let [datomic-config    (:datomic config)
        http-config  (:http config)]
    (-> (component/system-map
         :config   config
         :datomic (create-datomic datomic-config)
         :app     (create-app)
         :http    (create-http-server http-config))
        (component/system-using
         {:app {:datomic :datomic}})
        (component/system-using
         {:http {:app :app} }))))

(defn -main [& args]
  (let [config (conf/config)]
    (.start (system config))))

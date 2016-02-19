(ns ringi.http
  (:require [com.stuartsierra.component :as component]
            [immutant.web :as web]))

(defrecord HTTPServer [config app server]
  component/Lifecycle
  (start [component]
    (let [server (web/run (:handler app) config)]
      (assoc component :server server)))
  (stop [component]
    (when server
      (web/stop server)
      component)))

(defn create-http-server [config]
  (map->HTTPServer {:config config}))

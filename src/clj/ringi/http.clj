(ns ringi.http
  (:require [com.stuartsierra.component :as component]
            [ring.adapter.jetty :as jetty]))

(defrecord HTTPServer [config app server]
  component/Lifecycle
  (start [component]
    (let [server (jetty/run-jetty (:handler app) config)]
      (assoc component :server server)))
  (stop [component]
    (when server
      (.stop server)
      component)))

(defn create-http-server [config]
  (map->HTTPServer {:config config}))

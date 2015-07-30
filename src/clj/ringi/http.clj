(ns ringi.http
  (:require [com.stuartsierra.component :as component]
            [aleph.http :refer [start-server]]))

(defrecord HTTPServer [config app server]
  component/Lifecycle
  (start [component]
    (let [server (start-server (:handler app) config)]
      (assoc component :server server)))
  (stop [component]
    (when server
      (.close server)
      component)))

(defn create-http-server [config]
  (map->HTTPServer {:config config}))

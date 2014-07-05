(ns ringi.server
  (:require [com.stuartsierra.component :as     component])
  (:import [org.httpkit.server HttpServer RingHandler]))

(defn http-server
  "Lifted from HTTPKit to just create the freakin' server and nothing else"
  [handler {:keys [port thread ip max-body max-line worker-name-prefix queue-size]
            :or   {ip "0.0.0.0"  ; which ip (if has many ips) to bind
                   port 8090     ; which port listen incomming request
                   thread 4      ; http worker thread count
                   queue-size 20480 ; max job queued before reject to project self
                   worker-name-prefix "worker-" ; woker thread name prefix
                   max-body 8388608             ; max http body: 8m
                   max-line 4096}}]  ; max http inital line length: 4K
  (let [h (RingHandler. thread handler worker-name-prefix queue-size)
        s (HttpServer. ip port h max-body max-line)]
    s))

(defrecord HTTPServer [config app http]
  component/Lifecycle
  (start [component]
    (println ";; sup")
    (let [http (http-server (:handler app) config)]
      (.start http)
      (assoc component :http http)))

  (stop [component]
    (.stop http)
    component))

(defn create-http-server [config]
  (map->HTTPServer {:config config}))

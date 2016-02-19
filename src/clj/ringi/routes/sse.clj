(ns ringi.routes.sse
  (:require [clojure.tools.logging :as log]
            [compojure.route      :as route]
            [compojure.core       :refer [GET routes context let-routes]]
            [clojure.core.async   :refer [sub chan go-loop <! timeout sliding-buffer] :as async]
            [immutant.web.sse :as sse]
            [datomic.api   :as d]
            [cheshire.core :as json]
            [ringi.mapper  :refer [defmap]]
            [ringi.models.topic   :as topic]
            [ringi.util :refer [parse-uuid]]))

(defmap e->author [m]
  [:id :user/uid])

(defmap e->vote [m]
  [:choice {:from :_choices
            :fn #(get % :choice/uid)}
   :id    :db/id
   :value {:from :vote/value
           :fn name}
   :author {:from :vote/author
            :fn e->author}])

(defn subscribe-topic [ctx request]
  (let [tx-pub   (get-in ctx [:tx-listener :tx-pub])
        txes     (chan (sliding-buffer 8))
        topic-id (parse-uuid (get-in request [:params :tid]))
        conn     (get-in ctx [:datomic :conn])
        topic-e  (d/entid (d/db conn) [:topic/uid topic-id])
        tx-sub   (sub tx-pub topic-e txes)]
    (sse/as-channel request
      {:on-open (fn [stream]
                  (go-loop []
                    (let [[_ message] (<! txes)
                          {:keys [topic entity]} message
                          vote (d/entity (d/db conn) entity)]
                      (sse/send! stream {:data (json/generate-string  (e->vote vote))})
                      (recur))))
       :on-close (fn [stream {:keys [code reason]}]
                   (async/close! txes))})))

(defn sse-routes [ctx]
  (routes
   (GET "/topics/:tid/subscribe" request (subscribe-topic ctx request))))

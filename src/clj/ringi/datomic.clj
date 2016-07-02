(ns ringi.datomic
  (:require [ringi.schema :as schema]
            [datomic.api :as d]
            [com.stuartsierra.component :as component]))

(defrecord Datomic [uri conn]
  component/Lifecycle
  (start [component]
    (let [db (d/create-database uri)
          conn (d/connect uri)]
      (schema/migrate conn)
      (assoc component :conn conn)))

  (stop [component]
    (assoc component :conn nil)
    component))

(defn create-datomic [config]
  (map->Datomic {:uri (:uri config)}))

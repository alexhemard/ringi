(ns ringi.db
  (:require [ringi.config :refer [config]]
            [clojure.java.jdbc :as jdbc]
            [clojure.java.io :refer [resource]]
            [com.stuartsierra.component :as component])
  (:import [com.jolbox.bonecp BoneCPDataSource]))

;; TODO use BoneCP

(defn pooled-db
  [{:keys [classname subprotocol subname username password min-connections max-connections partitions increment]
    :as spec
    :or {classname "org.postgresql.Driver"
         min-connections 1
         max-connections 6
         partitions 4
         increment 2}}]
  (let [ds (doto (BoneCPDataSource.)
             (.setDriverClass classname)
             (.setJdbcUrl (str "jdbc:" subprotocol ":" subname))
             (.setMinConnectionsPerPartition min-connections)
             (.setMaxConnectionsPerPartition max-connections)
             (.setPartitionCount partitions)
             (.setAcquireIncrement increment)
             (.setUsername username)
             (.setPassword password))]
    {:datasource ds}))


;; (defn pooled-db
;;   [{:keys [classname subprotocol subname username password] :as other-spec }]
;;   (let [cpds (doto (ComboPooledDataSource.)
;;                (.setDriverClass classname)
;;                (.setJdbcUrl (str "jdbc:" subprotocol ":" subname))
;;                (.setUser username)
;;                (.setPassword password))]
;;     {:datasource cpds}))

(defrecord Database [spec datasource]
  component/Lifecycle
  (start [component]
    (println ";; Starting database")
    (merge component (pooled-db spec)))

  (stop [component]
    (println ";; Stopping database")
    (.close (:datasource component))
    component))

(defn create-database [spec]
  (map->Database {:spec spec}))

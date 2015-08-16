(ns ringi.db
  (:require  [cljs.core.async :refer [chan pub sub <! put!] :as async]
             [datascript :as d]
             [om.core :as om])
  (:require-macros
   [cljs.core.async.macros :refer [go go-loop]]))

(defn qe
  "If queried entity id, return single entity of first result"
  [q db & sources]
  (->> (apply d/q q db sources)
       ffirst
       (d/entity db)))

(defn qes
  "If queried entity ids, return all entities of result"
  [q db & sources]
  (->> (apply d/q q db sources)
       (map #(d/entity db (first %)))))

(defn bind
  [conn state key q & q-args]
  (let [k (d/squuid)]
    (om/update! state key (apply qes q @conn q-args))
    (d/listen! conn k (fn [tx-report]
                        (om/update! state key (apply qes q (:db-after tx-report) q-args))))

    state))

(defn unbind
  [conn state key]
  (d/unlisten! conn key))

(def schema
  {;; users
   
   :user/id           {:db/unique :db.unique/identity}
   :user/name         {:db/unique :db.unique/identity}
   :user/avatar       {}
   :user/me           {}

   ;; topics
   
   :topic/id          {:db/unique :db.unique/identity}
   :topic/title       {}
   :topic/description {}
   :topic/timestamp   {}
   :topic/author      {:db/valueType    :db.type/ref
                       :db/cardinality  :db.cardinality/one}
   :topic/choices     {:db/valueType    :db.type/ref
                       :db/cardinality  :db.cardinality/many
                       :db/isComponent  true}

   ;; items
   
   :choice/id         {:db/unique      :db.unique/identity}
   :choice/title      {}
   :choice/author     {:db/valueType   :db.type/ref
                       :db/cardinality :db.cardinality/one}
   :choice/timestamp  {}

   ;; comments
   
   :comments          {:db/valueType    :db.type/ref
                       :db/cardinality  :db.cardinality/many
                       :db/isComponent  true}
   :comment/body      {}
   :comment/author    {:db/valueType :db.type/ref}

  ;; votes
   
   :votes             {:db/valueType    :db.type/ref
                       :db/cardinality  :db.cardinality/many
                       :db/isComponent  true}
   :vote/id           {:db/unique :db.unique/identity}
   :vote/value        {}
   :vote/author       {:db/valueType :db.type/ref}})

(def conn
  (d/create-conn schema))

(comment
  (d/transact! conn [{:db/id -1
                      :topic/title "whatever"
                      :topic/id "pizza"
                      :topic/author {:user/name "alex"
                                     :user/id "55ce4c96-aa64-4cf3-820d-f38f98b7663a" }}
                     {:db/id -2
                      :topic/id "jazz"
                      :topic/title "swag"
                      :topic/author {:user/name "alex"
                                     :user/id "55ce4c96-aa64-4cf3-820d-f38f98b7663a"}}]))

(defn persist [tx state]
  (let [conn (:conn @state)]
    (d/transact! conn tx)))

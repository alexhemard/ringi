(ns ringi.db
  (:require  [cljs.core.async :refer [chan pub sub <! put!] :as async]
             [reagent.core :as r :refer [atom]]
             [datascript :as d])
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
  [conn state q & q-args]
  (let [k (d/squuid)]
    (reset! state (apply qes q @conn q-args))
    (d/listen! conn k (fn [tx-report]
                        (.log js/console (:vote/value (first (apply qes q (:db-after tx-report) q-args))))
                        (reset! state (apply qes q (:db-after tx-report) q-args))))
    (set! (.-__key state) k)
    state))

(defn unbind
  [conn state]
  (d/unlisten! conn (.-__key state)))

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

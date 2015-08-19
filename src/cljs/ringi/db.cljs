(ns ringi.db
  (:require  [cljs.core.async :refer [chan pub sub <! put!] :as async]
             [datascript :as d]
             [om.core :as om])
  (:require-macros
   [cljs.core.async.macros :refer [go go-loop]]))

(defn bind
  ([conn state key q qargs]
   (bind conn state key q qargs {}))
  ([conn state key q qargs opts]
   (let [data-fn (get :fn opts identity)]
     (om/update! state key (data-fn (apply d/q q @conn qargs)))
     (d/listen! conn key (fn [tx-report]
                           (om/update! state key (data-fn (apply d/q q (:db-after tx-report) qargs))))))
   state))

(defn unbind
  [conn state key]
  (om/update! state key nil)
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
   :topic/author      {:db/valueType   :db.type/ref
                       :db/cardinality :db.cardinality/one}
   :topic/choices     {:db/valueType   :db.type/ref
                       :db/cardinality :db.cardinality/many
                       :db/isComponent true}

   ;; items

   :choice/id         {:db/unique      :db.unique/identity}
   :choice/title      {}
   :choice/author     {:db/valueType   :db.type/ref
                       :db/cardinality :db.cardinality/one}
   :choice/timestamp  {}

   ;; comments

   :comments          {:db/valueType   :db.type/ref
                       :db/cardinality :db.cardinality/many
                       :db/isComponent true}
   :comment/id        {:db/unique      :db.unique/identity}
   :comment/content   {}
   :comment/author    {:db/valueType   :db.type/ref}

  ;; votes

   :votes             {:db/valueType   :db.type/ref
                       :db/cardinality :db.cardinality/many
                       :db/isComponent true}
   :vote/id           {:db/unique      :db.unique/identity}
   :vote/value        {}
   :vote/author       {:db/valueType   :db.type/ref}})

(def conn
  (d/create-conn schema))

(defn handle [tx state]
  (let [conn (:conn @state)]
    (d/transact! conn tx)))

(defn persist! [ch tx]
  (put! ch tx))

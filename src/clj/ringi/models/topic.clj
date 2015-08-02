(ns ringi.models.topic
    (:require [datomic.api  :as d]
              [ringi.query :refer [qe qes find-by]]
              [ringi.mapper :refer [defmap]]))

(defmap user->map [m]
  [:id   :user/user
   :name :user/name])

(defmap votes->map [m]
  [:author {:from :vote/author
            :fn user->map}
   :value  {:from :vote/value
            :fn name}])

(defmap choice->map [m]
  [:id     :choice/uid
   :author {:from :choice/author
            :fn user->map}
   :title  :choice/title    
   :votes {:from :votes
           :fn votes->map}])

(defmap topic->map [m]
  [:id          :topic/uid
   :title       :topic/title
   :description :topic/description
   :author      {:from :topic/author
                 :fn user->map}
   :choices     {:from :choices
                 :fn choice->map
                 :cardinality :many}])

(defmap topic->map [m]
  [:id          :topic/uid
   :title       :topic/title
   :description :topic/description
   :author      {:from :topic/author
                 :fn user->map}
   :choices     {:from        :choices
                 :fn          choice->map
                 :cardinality :many}])

(defn fetch [conn id]
  (let [db (d/db conn)]
    (qe '[:find ?t
          :in $ ?t
          :where [?t]]
        (d/db conn) id)))

(defn fetch-all [conn]
  (let [db (d/db conn)]
    (mapv first (qes '[:find ?t
                       :in $
                       :where [?t :topic/uid]]
                     db))))

(defn create [conn data]
  @(d/transact conn data))

(ns ringi.models.topic
    (:require [datomic.api  :as d]
              [ringi.query :refer [qe qes find-by]]
              [ringi.mapper :refer [defmap one many]]))

(defmap author->map
  (one :id :from :user/gid)
  (one :name :from :user/name))

(defmap votes->map
  (one :author :from :choice/author
               :fn author->map)
  (one :value  :from :vote/value))

(defmap choice->map
  (one :id     :from :choice/gid)
  (one :author :from :choice/author
               :fn author->map)
  (many :votes :from :choice/votes
               :fn votes->map))

(defmap topic->map
  (one  :id          :from :topic/gid)
  (one  :author      :from :topic/author
                     :fn author->map)
  (one  :title       :from :topic/title)
  (one  :description :from :topic/description)
  (many :choices     :from :topic/choices
                     :fn choice->map))

(defn find-topics [db]
  (qes '[:find ?t
         :where [?t :topic/title]]
       db))

(defn topic-leader [db t]
  (d/q '[:find (count ?vv)
         :in $ ?t
         :where [?t :topic/choices ?c]]
       db
       t))

(defn topics-count [db]
  (ffirst (d/q '[:find (count ?t)
                 :where [?t :topic/title]]
               db)))

(defn choices-count [db topic]
  (ffirst (d/q '[:find (count ?t)
                 :where [?t :topic/title]]
               db)))

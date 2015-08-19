(ns ringi.models.topic
    (:require [datomic.api  :as d]
              [ringi.query :refer [qe qes find-by]]
              [ringi.mapper :refer [defmap]]
              [ringi.util   :refer [not-blank?]]
              [clojure.string :refer [blank?]]
              [bouncer.core :as b]
              [bouncer.validators :as v]))

(defn validate [topic]
  (b/validate
   topic
   :topic/title       [v/required [not-blank? :message "Title cannot be blank."]]
   :topic/description [[not-blank? :message "Description cannot be blank."]]
   :choices [[v/every #(not-blank? (:choice/title %))
              :message "choices must have a title."]]))

(defn validate-update [topic]
  (b/validate
   topic
   :topic/title       [[not-blank? :message "Title cannot be blank."]]
   :topic/description [[not-blank? :message "Description cannot be blank."]]))

(defmap user->raw [m]
  [:id   :user/uid
   :name :user/name])

(defmap votes->raw [m]
  [:id      :db/id
   :author {:from :vote/author
            :fn user->raw}
   :value  {:from :vote/value
            :fn name}])

(defmap comment->raw [m]
  [:id      :comment/uid
   :author {:from :comment/author
            :fn user->raw}
   :content :comment/content])

(defmap choice->raw [m]
  [:id        :choice/uid
   :author   {:from :choice/author
              :fn user->raw}
   :title     :choice/title
   :comments {:from :comments
              :fn comment->raw
              :cardinality :many}
   :votes {:from :votes
           :fn votes->raw
           :cardinality :many}])

(defmap topic->raw [m]
  [:id          :topic/uid
   :title       :topic/title
   :description :topic/description
   :author      {:from :topic/author
                 :fn user->raw}
   :choices     {:from :choices
                 :fn choice->raw
                 :cardinality :many}])

(defmap raw->update [m]
  [:topic/title       :title
   :topic/description :description])

(defmap raw->choice [m]
  [:choice/title :title
   :choice/description :description])

(defmap raw->topic [m]
  [:topic/title       :title
   :topic/description :description
   :choices {:from       :choices
             :fn         raw->choice
             :cardinality :many}])

(defn fetch [conn id]
  (find-by (d/db conn) :topic/uid id))

(defn fetch-all [conn]
  (let [db (d/db conn)]
    (mapv first (qes '[:find ?t
                       :in $
                       :where [?t :topic/uid]]
                     db))))

(defn fetch-all-for-user [conn user]
  (let [db (d/db conn)]
    (mapv first (qes '[:find ?t
                       :in $ ?u
                       :where [?t :topic/author ?u]]
                     db user))))

(defn create [conn user topic]
  (let [[errors topic] (validate topic)]
    (if-not errors
      (let [eid (d/tempid :db.part/user)
            topic (assoc topic :db/id eid)
            {:keys [db-after tempids]} @(d/transact conn [[:topic/create user topic]])]
        (d/entity db-after (d/resolve-tempid db-after tempids eid)))
      {:errors errors})))

(defn modify [conn user id partial]
  (let [[errors topic] (validate-update partial)]
    (if-not errors
      (let [topic (assoc topic :db/id [:topic/uid id])
            {:keys [db-after tempids]} @(d/transact conn [topic])]
        (d/entity db-after [:topic/uid id]))
      {:errors errors})))

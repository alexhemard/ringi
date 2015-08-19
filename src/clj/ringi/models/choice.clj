(ns ringi.models.choice
  (:require [datomic.api :as d]
            [ringi.query :refer :all]
            [ringi.mapper :refer [defmap]]
            [ringi.models.comment :as comment]
            [ringi.util :refer [not-blank?]]
            [bouncer.core :as b]
            [bouncer.validators :as v]))

(defn validate [choice]
  (b/validate
   choice
   :choice/title  [v/required [not-blank? :message "Description cannot be blank."]]
   :choice/description [[not-blank? :message "Description cannot be blank."]]))

(defn validate-update [choice]
  (b/validate
   choice
   :choice/title       [[not-blank? :message "Title cannot be blank."]]
   :choice/description [[not-blank? :message "Description cannot be blank."]]))

(defmap raw->choice [m]
  [:choice/title       :title
   :choice/description :description])

(defn fetch [conn id]
  (find-by (d/db conn) :choice/uid id))

(defn fetch-all [conn]
  (let [db (d/db conn)]
    (mapv first (qes '[:find ?t
                       :in $
                       :where [?t :choice/uid]]
                     db))))

(defn create [conn user topic-id choice]
  (let [[errors choice] (validate choice)]
    (if-not errors
      (let [eid (d/tempid :db.part/user)
            choice (assoc choice :db/id eid)
            {:keys [db-after tempids]} @(d/transact conn [[:choice/create user [:topic/uid topic-id] choice]])]
        (d/entity db-after (d/resolve-tempid db-after tempids eid)))
      {:errors errors})))

(defn modify [conn user id partial]
  (let [[errors topic] (validate-update partial)]
    (if-not errors
      (let [choice (assoc topic :db/id [:choice/uid id])
            {:keys [db-after tempids]} @(d/transact conn [choice])]
        (d/entity db-after [:choice/uid id]))
      {:errors errors})))

(defn fetch-comments [conn choice-id]
  (comment/fetch-all conn [:choice/uid choice-id]))

(defn create-comment [conn user choice-id comment]
  (comment/create conn user [:choice/uid choice-id] comment))

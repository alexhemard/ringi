(ns ringi.models.comment
  (:require [datomic.api :as d]
            [ringi.query :refer :all]
            [ringi.mapper :refer [defmap]]
            [ringi.util   :refer [not-blank?]]
            [clojure.string :refer [blank?]]
            [bouncer.core :as b]
            [bouncer.validators :as v]))

(defn validate [comment]
  (b/validate
   comment
   :comment/content  [v/required [not-blank? :message "Comment cannot be blank."]]))

(defmap user->raw [m]
  [:name :user/name])

(defmap comment->raw [m]
  [:author  {:from :comment/author
             :fn   user->raw}
   :content :comment/content])

(defmap raw->comment [m]
  [:comment/content :content])

(defn fetch-all [conn thing]
  (let [db (d/db conn)]
    (mapv first (qes '[:find ?c
                       :in $
                       :where [?t :comments ?c]]
                     db))))

(defn create [conn user thing comment]
  (let [[errors comment] (validate comment)]
    (if-not errors
      (let [eid (d/tempid :db.part/user)
            comment (assoc comment :db/id eid)
            {:keys [db-after tempids]} @(d/transact conn [[:comment/create user thing comment]])]
        (d/entity db-after (d/resolve-tempid db-after tempids eid)))
      {:errors errors})))

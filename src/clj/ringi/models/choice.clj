(ns ringi.models.choice
  (:require [datomic.api :as d]
            [ringi.query :refer :all]))

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
            choice (assoc topic :db/id eid)
            {:keys [db-after tempids]} @(d/transact conn [[:choice/create user topic-id choice]])]
        (d/entity db-after (d/resolve-tempid db-after tempids eid)))
      {:errors errors})))

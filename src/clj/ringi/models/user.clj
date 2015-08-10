(ns ringi.models.user
  (:require [datomic.api :as d]
            [ringi.query :refer [qe find-by qes]]
            [ringi.mapper :refer [defmap]]
            [crypto.password.pbkdf2 :as p]))

(defmap user->map [m]
  [:id   :user/user
   :name :user/name])

(defn- fetch-all [conn query])

(defn fetch [conn id]
  (find-by (d/db conn) :user/uid id))

(defn find-by-twitter-uid [conn uid])

(defn find-or-create-by-twitter-oauth [conn oauth])

(defn create [conn username email password]
  (let [user-id  (d/tempid :db.part/user)
        password (p/encrypt password)
        user-tx  [:user/register user-id username email password]
        {:keys [tempids db-after]} @(d/transact conn [user-tx])
        user  (d/resolve-tempid db-after tempids user-id)]
    (d/entity db-after user)))

(defn find-by-name-or-email [conn name-or-email]
  (let [db (d/db conn)]
    (d/entity
     db
     (ffirst
      (d/q '[:find ?e
             :in $ ?ne
             :where (or [?e :user/name  ?ne]
                        [?e :user/email ?ne])]
           db
           name-or-email)))))

(defn find-by-access-token [conn access-token])

(defn create-provider [conn access-token & user]
  (let [token    (:oauth-token access-token)
        secret   (:oauth-token-secret access-token)
        name     (:screen_name access-token)
        uid      (:user_id access-token)
        user     (first user)
        new-user (when-not user (d/tempid :db/part/user))
        user     (or user new-user)
        user-tx  [:constructUser {:db/id user
                                  :user/name name}]
        txes   [[:createProvider user
                 :provider.type/twitter
                 {:provider/token token
                  :provider/secret secret
                  :provider/uid uid}]]
        txes (if new-user (cons user-tx txes) txes)]
    (d/transact conn txes)))

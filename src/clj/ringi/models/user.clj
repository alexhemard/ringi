(ns ringi.models.user
  (:require [datomic.api :as d]
            [ringi.query :refer [qe find-by qes]]
            [ringi.mapper :refer [defmap one many]]
            [crypto.password.pbkdf2 :as p]))

(defmap user->map
  (one :id :from :user/uid)
  (one :name :from :user/name)
  (one :email :from :user/email))

(defn- fetch-all [conn query])

(defn find-by-id [conn id]
  (find-by (d/db conn) :user/uid id))

(defn find-by-twitter-uid [conn uid]
  (let [db (d/db conn)]
    (d/entity db
              (ffirst
               (d/q '[:find ?u
                      :in $ ?uid
                      :where [?u :twitter/oauth ?o]
                      [?o :twitter/uid ?uid]]
                    db
                    uid)))))

(defn find-or-create-by-twitter-oauth [conn oauth]
  (let [db (d/db conn)]
    (if-let [user (find-by-twitter-uid db (:twitter/uid oauth))]
      user
      (do
        (d/transact conn [[:touchUserByTwitterOauth oauth]])
        (find-by-twitter-uid db (:twitter/uid oauth))))))

(defn create
  [conn {:keys [username email password] :as params}]
  (d/transact
   conn
   [{:db/id         (d/tempid :db.part/user)
     :user/uid      (d/squuid)
     :user/name      username
     :user/email     email
     :user/password (p/encrypt password)}]))

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

(defn find-by-access-token [conn access-token]
  (let [db (d/db conn)] 
    (ffirst
     (d/q '[:find ?u
            :in $ ?t
            :where [?u :user/providers ?p]
            [?p :provider/type :provider.type/twitter]
            [?p :provier/token ?t]]
          db
          (:oauth-token access-token)))))

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

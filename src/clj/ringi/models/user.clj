(ns ringi.models.user
    (:require [clojure.java.jdbc      :as jdbc]
              [honeysql.core          :as sql]
              [honeysql.helpers       :refer :all]
              [crypto.password.pbkdf2 :as p]))


(defn- fetch-all [db query]
  (-> (jdbc/query
       db (-> query
              (sql/format :quoting :ansi)))))

(defn fetch [db id]
  (first
   (fetch-all
    db
    (-> (select :users.*)
        (from   :users)
        (where  [:= :id id])))))

(defn fetch-by-twitter-uid [db uid]
  (first
   (fetch-all
    db
    (-> (select :users.* [:providers_users.uid :uid])
        (from :users)
        (join :providers_users ["=" :providers_users.user_id :users.id]
              :providers ["=" :providers.id :providers_users.provider_id])
        (where [:and
                ["=" :providers.name "twitter"]
                ["=" :providers_users.uid uid]])))))


(defn create [db {:keys [password] :as data}]
  (first (jdbc/insert! db :users data)))

(defn create-provider-user [db data]
  (first (jdbc/insert! db :providers_users data)))

(defn create-provider-for-user [db access-token & user]
  (jdbc/with-db-transaction [db db]
    (let [token    (:oauth-token access-token)
          secret   (:oauth-token-secret access-token)
          name     (:screen_name access-token)
          uid      (:user_id access-token)
          user     (if user user (create db {:name name}))
          provider (first (jdbc/insert! db {:user_id (:id user)
                                            :provider_id #uuid "c87c7d3d-ee38-4897-a129-9f43ea21a46d"
                                            :uid uid
                                            :token token
                                            :secret secret}))]
      user)))


(defn fetch-or-create-by-access-token [db access-token & current-user]
  (let [user (fetch-by-twitter-uid db (:user_id access-token))]
    (if user
      user
      (create-provider-for-user db access-token current-user))))

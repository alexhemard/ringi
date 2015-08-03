(ns ringi.auth
  (:require [compojure.core         :refer [routes context GET]]
            [ring.util.response     :refer [redirect]]
            [ringi.models.user      :as    user]
            [clojure.tools.logging  :as    log]
            [ringi.util             :refer [parse-uuid]]
            [oauth.client           :as    oauth]))

(defn current-user [req]
  (:user req))

(defn wrap-user [f ctx]
  (fn [req]
    (let [conn (get-in ctx [:datomic :conn])
          user-id (get-in req [:session :user_id])
          user-id (parse-uuid user-id)
          user    (when user-id (user/fetch conn user-id))]
      (if user
        (f (assoc req :user user))
        (f req)))))

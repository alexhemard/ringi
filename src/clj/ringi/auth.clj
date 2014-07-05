(ns ringi.auth
  (:require [compojure.core     :refer [routes context GET]]
            [ring.util.response :refer [redirect]]
            [ringi.models.user  :as    user]
            [ringi.util         :refer [parse-uuid]]
            [oauth.client       :as    oauth]))

(defn current-user [req]
  (::user req))

(defn wrap-user [f ctx]
  (fn [req]
    (let [db (:db ctx)
          session (:session req)
          user-id (parse-uuid (:user_id session))
          user    (user/fetch db user-id)]
      (if user
        (f (assoc req :user user))
        (f req)))))

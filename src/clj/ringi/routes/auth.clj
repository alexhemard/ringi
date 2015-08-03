(ns ringi.routes.auth
  (:require [compojure.core         :refer [routes context GET POST]]
            [ring.util.response     :refer [redirect response status]]
            [ringi.models.user      :as    user]
            [datomic.api            :as d]
            [ringi.util             :refer [parse-uuid json-response unauthorized-response]]
            [crypto.password.pbkdf2 :refer [check]]
            [oauth.client           :as    oauth]))

(def consumer (oauth/make-consumer "***REMOVED***"
                                   "***REMOVED***"
                                   "https://api.twitter.com/oauth/request_token"
                                   "https://api.twitter.com/oauth/access_token"
                                   "https://api.twitter.com/oauth/authorize"
                                   :hmac-sha1))

(defn login [ctx params session]
  (let [conn                        (get-in ctx [:datomic :conn])
        {:keys [username password]}  params
        user (user/find-by-name-or-email conn username)]
    (if (and user (check password (:user/password user)))
      (-> (redirect "/")
          (assoc :session (assoc session :user_id (:user/uid user))))
      (unauthorized-response))))

(defn logout [ctx session]
  (-> (redirect "/")
      (assoc :session (dissoc session :user_id))))

(defn register [ctx params session]
  (let [conn (get-in ctx [:datomic :conn])
        {:keys [username email password]} params
        user (d/entity (d/db conn) @(user/create conn username email password))]
    (when user
      (-> (json-response user)
          (assoc :session (assoc :user_id session (:user/uid user)))))))

(defn twitter-login [ctx params session callback]
  (let [request-token (oauth/request-token consumer callback)
        session       (assoc session :twitter_oauth request-token)]
    (-> (redirect (oauth/user-approval-uri consumer (:oauth_token request-token)))
        (assoc :session session))))

(defn twitter-auth [ctx params session user]
  (let [conn         (get-in ctx [:datomic :conn])
        access-token (oauth/access-token consumer
                                         (:twitter_oauth session)
                                         (:oauth_verifier params))
        token       (:oauth_token access-token)
        secret      (:oauth_token_secret access-token)
        screen-name (:screen_name access-token)
        uid         (:user_id access-token)
        user        (user/find-or-create-by-twitter-oauth conn {:twitter/uid uid
                                                                :oauth/token token
                                                                :oauth/secret secret
                                                                :twitter/name screen-name})]
    (if user
      (-> (redirect "/")
          (assoc :session (-> session
                              (assoc  :user_id (:user/uid user))
                              (dissoc :twitter_oauth))))
           (redirect "/login"))))

(defn reset-password [ctx]
  "TODO")


(defn auth-routes [ctx]
  (routes
   (POST "/login"     {:keys [params session]} (login ctx params session))
   (POST "/register"  {:keys [params session]} (register ctx params session))
   (GET "/logout"     {:keys [session]}        (logout ctx session))
   (context "/twitter" []
     (GET "/login" {:keys [params session server-name port]}
       (twitter-login ctx  params session (str "http://" server-name "/twitter/auth")))
     (GET "/auth"  {:keys [params session user]}
       (twitter-auth  ctx  params session user)))))



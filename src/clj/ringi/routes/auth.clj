(ns ringi.routes.auth
  (:require [compojure.core     :refer [routes context GET POST]]
            [ring.util.response :refer [redirect]]
            [ringi.models.user  :as    user]
            [ringi.util         :refer [parse-uuid]]
            [oauth.client       :as    oauth]))


(def consumer (oauth/make-consumer "***REMOVED***"
                                   "***REMOVED***"
                                   "https://api.twitter.com/oauth/request_token"
                                   "https://api.twitter.com/oauth/access_token"
                                   "https://api.twitter.com/oauth/authenticate"
                                   :hmac-sha1))

(defn login [ctx]

)

(defn logout [ctx session]
  (-> (redirect "/")
      (assoc :session (dissoc session :user))))

(defn register [ctx]

)

(defn twitter-login [ctx params session]
  (let [request-token (oauth/request-token consumer)
        session       (assoc session :twitter_oauth request-token)]
    (-> (redirect (oauth/user-approval-uri consumer (:oauth_token request-token)))
        (assoc :session session))))

(defn twitter-auth [ctx params session user]
  (let [db           (:db ctx)
        access-token (oauth/access-token consumer
                                         (:twitter_oauth session)
                                         (:oauth_verifier params))
        user         (user/fetch-or-create-by-access-token db access-token user)]
    (if user
      (-> (redirect "/")
          (assoc :session (-> session
                              (assoc  :user_id (:id user))
                              (dissoc :twitter_oauth))))
           (redirect "/login"))))

(defn reset-password [ctx]
  "TODO")


(defn auth-routes [ctx]
  (routes
   (POST "/register" [] (register))
   (POST "/login"    [] (login))
   (GET "/logout"    [session] (logout ctx session))
   (context "/twitter" []
     (GET "/login" {:keys [params session]} (twitter-login ctx params session))
     (GET "/auth" {:keys [params session user]} (twitter-auth ctx params session user)))))

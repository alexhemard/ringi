(ns ringi.routes.api
  (:require [compojure.handler  :as handler]
            [compojure.route    :as route]
            [compojure.core     :refer [GET PUT DELETE POST routes context let-routes]]
            [ring.util.response :as resp]
            [ringi.models.vote  :as vote]
            [ringi.util  :refer [parse-uuid parse-int unauthorized! json-response]]
            [ringi.auth  :refer [current-user]]))

(defn post-choice [ctx vote-id user body]
  (if user
    (json-response {:todo "true"})
    (unauthorized!)))

(defn put-choice [ctx choice-id user body]
  (if user
    (json-response {:todo "true"})
    (unauthorized!)))

(defn post-decision [ctx choice-id user body]
  (if user 
    (json-response {:todo "true"})
    (unauthorized!)))

(defn get-votes [ctx user page per-page]
  (let [db       (:db ctx)
        page     (or (parse-int page) 1)
        per-page (or (parse-int per-page) 25)]
    (if user
      (let [votes (vote/fetch-all-for-user db (:id user) page per-page)]
        (json-response {:page page
                        :per-page per-page
                        :data votes}))
      (unauthorized!))))

(defn get-vote [ctx id]
  (let [db (:db ctx)
        uuid (parse-uuid id)]
    (if uuid
      (if-let [vote (vote/fetch db uuid)]
        (json-response vote)
        (resp/not-found nil))
      (resp/not-found nil))))

(defn post-vote [ctx body]
  (let [db (:db ctx)]
    
))

(defn put-vote [ctx id body]
  
  
)

(defn delete-vote [ctx id user])

(defn api-routes [ctx]
  (routes
   (context "/votes" {:keys [body params session user] :as req}
     (GET    "/" [p pp] (get-votes ctx user p pp))
     (POST   "/" [] (post-vote ctx user body))
     (context "/:id" [id]
       (GET "/" [] (get-vote ctx id))
       (PUT "/" [] (put-vote ctx user id {}))
       (DELETE "/" [] (delete-vote ctx user id))
       (context "/choices" []
         (POST "/" [] (post-choice ctx user body)))))
   (context "/choices/:id" {:keys [body params session user] :as req}
     (PUT "/" [id] (put-choice ctx user id body))
     (POST "/decisions" [id] (post-decision ctx user id body)))))

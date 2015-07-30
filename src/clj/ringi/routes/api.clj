(ns ringi.routes.api
  (:require [compojure.handler  :as handler]
            [compojure.route    :as route]
            [compojure.core     :refer [GET PUT DELETE POST routes context let-routes]]
            [ring.util.response :as resp]
            [ringi.models.topic  :as topic]
            [ringi.util  :refer [parse-uuid parse-int unauthorized! json-response]]
            [ringi.auth  :refer [current-user]]))

(defn post-choice [ctx vote-id user body]
  (let [db (:db ctx)]
    (if user
      (json-response {:todo "true"})
      (unauthorized!))))

(defn put-choice [ctx choice-id user body]
  (if user
    (json-response {:todo "true"})
    (unauthorized!)))

(defn post-votes [ctx choice-id user body]
  (if user
    (json-response {:todo "true"})
    (unauthorized!)))

(defn get-topics [ctx page per-page]
  (let [db       (get-in ctx [:datomic :db])
        page     (or (parse-int page) 1)
        per-page (or (parse-int per-page) 25)
        data     (map topic/topic->map (topic/find-topics db))]

    (json-response {:page page
                    :per-page per-page
                    :total (count data)
                    :data data})))

(defn get-topic [ctx id]
  (let [db (:db ctx)
        uuid (parse-uuid id)]
    (if uuid
      (resp/not-found nil))))

(defn post-topics [ctx body]
  (let [db (:db ctx)]))

(defn put-topic [ctx id body])

(defn delete-topic [ctx id user])

(defn api-routes [ctx]
  (routes
   (context "/topics" {:keys [body params session user] :as req}
     (GET    "/" [p pp] (get-topics ctx user p pp))
     (POST   "/" [] (post-topics ctx user body))
     (context "/:vid" [vid]
       (GET "/" [] (get-topic ctx vid))
       (PUT "/" [] (put-topic ctx user vid {}))
       (DELETE "/" [] (delete-topic ctx user vid))
       (context "/choices" []
         (POST "/" [] (post-choice ctx user body)))))
   (context "/choices/:cid" {:keys [body params session user] :as req}
     (PUT "/" [vid cid] (put-choice ctx user cid body))
     (POST "/votes" [vid cid] (post-votes ctx user cid body)))))

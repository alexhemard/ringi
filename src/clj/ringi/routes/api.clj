(ns ringi.routes.api
  (:require [compojure.handler  :as handler]
            [compojure.route    :as route]
            [compojure.core     :refer [GET PUT DELETE POST PATCH routes context let-routes]]
            [ring.util.response :refer [response created status not-found] :as resp]
            [ringi.models.topic   :as topic]
            [ringi.models.choice  :as choice]
            [ringi.models.vote    :as vote]
            [ringi.models.comment :as comment]
            [ringi.util  :refer [parse-uuid parse-int unauthorized! json-response]]
            [ringi.auth  :refer [current-user]]))

(defn get-topics [ctx user]
  (if user
    (let [conn (get-in ctx [:datomic :conn])
          topics (map topic/topic->raw (topic/fetch-all-for-user conn (:db/id user)))]
      (response {:total (count topics)
                 :data  topics}))
    (unauthorized!)))

(defn get-topic [ctx id]
  (let [conn  (get-in ctx [:datomic :conn])
        uid   (parse-uuid id)
        topic (when uid (topic/fetch conn uid))]
    (if topic
      (json-response (topic/topic->raw topic))
      (not-found "Topic not found."))))

(defn post-topic [ctx user body]
  (if user
    (let [conn (get-in ctx [:datomic :conn])
          body (topic/raw->topic body)
          {:keys [errors] :as topic} (topic/create conn (:db/id user) body)]
      (if-not errors
        (-> (created (str "/v1/topics/" (:topic/uid topic))))
        (-> (response {:errors errors})
            (status 422))))
    (unauthorized!)))

(defn post-topic-update [ctx user topic-id body]
  (let [conn (get-in ctx [:datomic :conn])
        topic-id (parse-uuid topic-id)
        partial (topic/raw->update body)
        {:keys [errors] :as topic} (topic/update conn (:db/id user) topic-id partial)]
    (if-not errors
      (-> (response nil)
          (status 204))
      (-> (response {:errors errors})
          (status 422)))))

(defn delete-topic [ctx id]
  (let [conn (get-in ctx [:datomic :conn])]
    (-> (response nil)
        (status 501))))

(defn post-choice [ctx user topic-id body]
  (if user
    (let [conn (get-in ctx [:datomic :conn])
          topic-id (parse-uuid topic-id)
          body (choice/raw->choice body)
          {:keys [errors] :as choice} (choice/create conn (:db/id user) topic-id body)]
      (if-not errors
        (-> (created (str "/v1/choices/" (:choice/uid choice))))
        (-> (response {:errors errors})
            (status 422))))
    (unauthorized!)))


(defn post-choice-update [ctx user choice-id body]
  (if user
    (let [conn (get-in ctx [:datomic :conn])
          choice-id (parse-uuid choice-id)
          partial (choice/raw->choice body)
          {:keys [errors] :as topic} (choice/update conn (:db/id user) choice-id partial)]
      (if-not errors
        (-> (response nil)
            (status 204))
        (-> (response {:errors errors})
            (status 422))))
    (unauthorized!)))    

(defn get-choice-comments [ctx choice-id]
  (let [conn (get-in ctx [:datomic :conn])
        choice-id (parse-uuid choice-id)
        comments (map comment/comment->raw (choice/fetch-comments conn choice-id))]
    (-> (response {:total (count comments)
                   :data comments})
        (status 200)))) 

(defn post-choice-comment [ctx user choice-id body]
  (if user
    (let [conn      (get-in ctx [:datomic :conn])
          body      (comment/raw->comment body)
          choice-id (parse-uuid choice-id)
          {:keys [errors] :as result} (choice/comment conn (:db/id user) choice-id body)]
      (if-not errors
        (-> (response nil)
            (status 201))
        (-> (response {:errors errors})
            (status 422))))
    (unauthorized!)))

(defn delete-comment [ctx comment-id]
  (let [conn (get-in ctx [:datomic :conn])]
      (-> (response nil)
          (status 501))))

(defn post-vote [ctx user choice-id value]
  (if user
    (let [conn      (get-in ctx [:datomic :conn])
          choice-id (parse-uuid choice-id)
          {:keys [errors] :as result} (vote/vote conn (:db/id user) [:choice/uid choice-id] value)]
      (if-not errors 
        (-> (response nil)
            (status 204))
        (-> (response {:errors errors})
            (status 422))))
    (unauthorized!)))

(defn api-routes [ctx]
  (routes
   (context "/topics" {:keys [body params session user] :as req}
     (GET   "/" [] (get-topics ctx user))
     (POST  "/" [] (post-topic ctx user body))
     (context "/:tid" [tid]
       (GET    "/" [] (get-topic ctx tid))
       (POST   "/" [] (post-topic-update ctx user tid body))
       (DELETE "/" [] (delete-topic ctx user tid))
       (context "/choices" []
         (POST "/" [] (post-choice ctx user tid body)))))
   (context "/choices/:cid" {:keys [body params session user] :as req}
     (POST "/"         [cid]       (post-choice-update ctx user cid body))
     (POST "/votes"    [cid value] (post-vote ctx user cid value))
     (GET  "/comments" [cid      ] (get-choice-comments ctx cid))
     (POST "/comments" [cid]       (post-choice-comment ctx user cid body)))
   (context "/comments/:cid" {:keys [body params session user] :as req}
     (DELETE "/" [cid] (delete-comment ctx user cid)))))

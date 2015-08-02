(ns ringi.routes.api
  (:require [compojure.handler  :as handler]
            [compojure.route    :as route]
            [compojure.core     :refer [GET PUT DELETE POST PATCH routes context let-routes]]
            [ring.util.response :refer [response created status not-found] :as resp]
            [ringi.models.topic  :as topic]
            [ringi.util  :refer [parse-uuid parse-int unauthorized! json-response]]
            [ringi.auth  :refer [current-user]]))

(defn get-topics [ctx user]
  (let [conn (get-in ctx [:datomic :conn])
        topics (map topic/topic->map (topic/fetch-all conn))]
    (json-response {:total (count topics)
                    :data  topics})))

(defn get-topic [ctx id]
  (let [conn (get-in ctx [:datomic :conn])
        uid (parse-uuid id)
        topic (topic/fetch conn [:topic/uid uid])]
    (println "hey>>" uid)
    (if topic
      (json-response (topic/topic->map topic))
      (resp/not-found "Topic not found."))))

(defn post-topic [ctx user body]
  (let [conn (get-in ctx [:datomic :conn])
        topic-id "nowhere"]
    (-> (created (str "/v1/topics/" topic-id))
        (status 501))))

(defn post-topic-update [ctx user topic-id body]
  (let [conn (get-in ctx [:datomic :conn])]
    (-> (response nil)
        (status 501))))    

(defn delete-topic [ctx id]
  (let [conn (get-in ctx [:datomic :conn])]
    (-> (response nil)
        (status 501))))

(defn post-choice [ctx user topic-id body]
  (let [conn (get-in ctx [:datomic :conn])]
    (if user
      (-> (response nil)
          (status 501))
      (unauthorized!))))

(defn post-choice-update [ctx attr user choice-id body]
  (let [conn (get-in ctx [:datomic :conn])]
    (if user
      (resp/status 501)
      (unauthorized!))))

(defn post-vote [ctx user choice-id value]
  (let [conn (get-in ctx [:datomic :conn])]    
    (if user
      (-> (response nil)
          (status 501))
      (unauthorized!))))

(defn api-routes [ctx]
  (routes
   (context "/topics" {:keys [body params session user] :as req}
     (GET    "/" [] (get-topics ctx user))
     (POST   "/" [] (post-topic ctx user body))
     (context "/:tid" [tid]
       (GET "/" [] (get-topic ctx tid))
       (POST "/" [] (post-topic-update ctx user tid body))
       (DELETE "/" [] (delete-topic ctx user tid))
       (context "/choices" []
         (POST  "/" [] (post-choice  ctx user body)))))
   (context "/choices/:cid" {:keys [body params session user] :as req}
     (POST "/" [cid] (post-choice-update ctx user cid body))
     (POST "/votes" [cid value] (post-vote ctx user cid value)))))

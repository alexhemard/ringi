(ns ringi.test.routes.api-test
  (:require [clojure.test :refer :all]
            [cheshire.core :as json]
            [datomic.api :as d]
            [ringi.models.topic  :as topic]
            [ringi.models.choice :as choice]
            [ringi.models.vote   :as vote]
            [peridot.core :refer :all]
            [kerodon.test :refer :all]
            [cheshire.core :as json]
            [ringi.test.helper :refer [fixtures default-fixture *system*]]))

(use-fixtures :each default-fixture)

(defn login [state user pass]
  (request state "/login"
           :request-method :post
           :params {:username user
                    :password pass}))

(deftest get-topics
  (let [app  (get-in *system* [:app :handler])]
    (-> (session app)
        (login "alex" "password")        
        (request "/v1/topics"
                 :request-method :get)
        (has (status? 200))
        ((fn [state]
           (let [topics (get-in state [:response :body])
                 topics (:data (json/parse-string topics true))]
              (is (= 2 (count topics)))
              (is (every? #(= (get-in % [:author :name]) "alex") topics))))))))

(deftest get-topic
  (let [topic-id (:topic/uid (first (:topics fixtures)))
        req   {:request-method :get
               :uri (str "/v1/topics/" topic-id)
               :session {:user_id 1}}
        app   (get-in *system* [:app :handler])
        resp  (app req)
        topic (json/parse-string (:body resp) true)]
    (is (= 200 (:status resp)))
    (is (= 2 (count (get topic :choices))))
    (is (= 3 (count (get-in topic [:choices 0 :votes] ))))))

(deftest post-topic
  (let [topic-body {:title       "hello"
                    :description "testing"
                    :choices [{:title "test"
                               :description "testing again"}
                              {:title "test2"
                               :description "me again"}]}
        app       (get-in *system* [:app :handler])]
    (-> (session app)
        (login "alex" "password")
        (content-type "application/json")
        (request "/v1/topics" :request-method :post
                 :body (json/generate-string topic-body))
        (has (status? 201))
        (follow-redirect)
        (has (status? 200)))))

(deftest post-topic-update
  (let [partial    {:title       "jazz"}
        conn       (get-in *system* [:datomic :conn])
        app        (get-in *system* [:app :handler])
        topic (first (topic/fetch-all conn))
        tid        (:topic/uid topic)]
    (-> (session app)
        (login "alex" "password")
        (content-type "application/json")
        (request (str "/v1/topics/" tid)
                 :request-method :post
                 :body (json/generate-string partial))
        (has (status? 204))
        (request (str "/v1/topics/" tid)
                 :request-method :get)
        ((fn [state]
           (let [body (get-in state [:response :body])
                 topic (json/parse-string body true)]
             (is (= "jazz" (:title topic)))))))))

(deftest post-choice
  (let [body  {:title       "new-choice"
               :description "itsreal"}
        app   (get-in *system* [:app :handler])
        conn  (get-in *system* [:datomic :conn])
        topic (first (topic/fetch-all conn))
        tid   (:topic/uid topic)]
    (-> (session app)
        (login "alex" "password")
        (content-type "application/json")
        (request (str "/v1/topics/" tid "/choices")
                 :request-method :post
                 :body (json/generate-string body))
        (has (status? 201))
        (follow-redirect)
        (has (status? 200)))))

(deftest post-choice-update
  (let [partial {:title       "updated-choice"}
        app   (get-in *system* [:app :handler])
        conn  (get-in *system* [:datomic :conn])
        choice (first (choice/fetch-all conn))
        cid   (:choice/uid choice)
        tid   (get-in choice [:_choices :topic/uid])]
    (-> (session app)
        (login "alex" "password")
        (content-type "application/json")
        (request (str "/v1/choices/" cid)
                 :request-method :post
                 :body (json/generate-string partial))
        (has (status? 204))
        (request (str "/v1/topics/" tid)
                 :request-method :get)
        ((fn [state]
           (let [body (get-in state [:response :body])
                 topic (json/parse-string body true)
                 choice (first (filter #(= (str cid) (:id %)) (:choices topic)))]
             (is (= "updated-choice" (:title choice)))))))))

(deftest post-vote
  (let [app     (get-in *system* [:app :handler])
        conn    (get-in *system* [:datomic :conn])
        choice  (first (choice/fetch-all conn))
        cid     (:choice/uid choice)
        tid     (get-in choice [:_choices :topic/uid])
        votes   (:votes choice)
        vote    (first (filter #(= "alex" (get-in % [:vote/author :user/name])) votes))
        new-val (if (= "yes" (name (:vote/value vote))) "no" "yes")]
    (-> (session app)
        (login "alex" "password")
        (request (str "/v1/choices/" cid "/votes")
                 :request-method :post
                 :params {:value new-val})
        (has (status? 204))
        (request (str "/v1/topics/" tid)
                 :request-method :get)
        ((fn [state]
           (let [body   (get-in state [:response :body])
                 topic  (json/parse-string body true)
                 choice (first (filter #(= (str cid) (:id %)) (:choices topic)))]
             (is (= new-val (:value (first
                                     (filter #(= "alex" (get-in % [:author :name])) (:votes choice))))))))))))

(deftest get-choice-comments
  (let [body   {:content "I love pizza"}
        app    (get-in *system* [:app :handler])
        choice (get-in fixtures [:choices 0])
        cid    (:choice/uid choice)]
    (-> (session app)
        (request (str "/v1/choices/" cid "/comments")
                 :request-method :get)
        (has (status? 200))
        ((fn [state]
            (let [body (get-in state [:response :body])
                  comments (:data (json/parse-string body true))]
              (is (= 0 (count comments)))))))))

(deftest post-choice-comment
  (let [body   {:content "I love pizza"}
        app    (get-in *system* [:app :handler])
        choice (get-in fixtures [:choices 0])
        cid    (:choice/uid choice)]
    (-> (session app)
        (login "alex" "password")
        (content-type "application/json")
        (request (str "/v1/choices/" cid "/comments")
                 :request-method :post
                 :body (json/generate-string body))
        (has (status? 201))
        (request (str "/v1/choices/" cid "/comments")
                 :request-method :get)
        ((fn [state]
            (let [body (get-in state [:response :body])
                  comments (:data (json/parse-string body true))]
              (is (some #(and (= "I love pizza"(:content %))
                              (= "alex" (get-in % [:author :name])))
                        comments))))))))

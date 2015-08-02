(ns ringi.test.routes.api-test
  (:require [clojure.test :refer :all]
            [cheshire.core :as json]
            [ringi.models.topic :as topic]
            [ringi.test.helper :refer [fixtures default-fixture *system*]]))

(use-fixtures :each default-fixture)

(deftest post-topic
  (let [topic-body {:title       "hello"
                    :description "testing"
                    :choices [{:title "test"
                               :description "testing again"}
                              {:title "test2"
                               :description "me again"}]}
        request {:request-method :post
                 :uri "/v1/topics"
                 :body topic-body
                 :session {:user_id 1}}
        app     (get-in *system* [:app :handler])
        response (app request)]
    (is (= 501 (:status response)))))

(deftest get-topics
  (let [request {:request-method :get
                 :uri "/v1/topics"
                 :session {:user_id 1}}
        app     (get-in *system* [:app :handler])
        response (app request)
        body (json/parse-string (:body response))
        topics (get body "data")]
    (is (= 200 (:status response)))
    (is (= (count (:topics fixtures)) (count topics)))))


(deftest get-topic
  (let [topic-id (:topic/uid (first (:topics fixtures)))
        request {:request-method :get
                 :uri (str "/v1/topics/" topic-id)
                 :session {:user_id 1}}
        app      (get-in *system* [:app :handler])
        response (app request)
        topic    (json/parse-string (:body response))]
    (is (= 200 (:status response)))
    (is (= 2 (count (get topic "choices"))))
    (is (= 4 (count (get-in topic ["choices" 1 "votes"] ))))))

(deftest post-topic
  (let [topic {:title       "testing"
               :description "test123"
               :choices [{:title "choice1"
                          :description "whatever"}
                         {:title "choice2"
                          :description "morestuff"}]}
        request  {:request-method :post
                  :uri "/v1/topics"
                  :body topic}
        app      (get-in *system* [:app :handler])
        response (app request)]
    (is (= 201 (:status response)))))


(deftest post-topic-update
  (let [partial   {:title       "new"
                   :description "poop"}
        conn      (get-in *system* [:datomic :conn])
        topic     (first (topic/fetch-all conn))
        tid       (:topic/uid topic)
        request   {:request-method :post
                   :uri (str "/v1/topics/" tid)
                   :body partial}
        app       (get-in *system* [:app :handler])
        response  (app request)
        new-topic (topic/fetch conn [:topic/uid tid])]
    (is (= "new" (:topic/title new-topic)))
    (is (= "poop" (:topic/description new-topic)))    
    (is (= 200 (:status response)))))

(deftest post-topic-update
  (let [partial   {:title       "jazz"
                   :description "swag"}
        conn      (get-in *system* [:datomic :conn])
        topic     (first (topic/fetch-all conn))
        tid       (:topic/uid topic)
        request   {:request-method :post
                   :uri (str "/v1/topics/" tid)
                   :body partial}
        app       (get-in *system* [:app :handler])
        response  (app request)
        new-topic (topic/fetch conn [:topic/uid tid])]
    (is (= "new" (:topic/title new-topic)))
    (is (= "poop" (:topic/description new-topic)))    
    (is (= 200 (:status response)))))



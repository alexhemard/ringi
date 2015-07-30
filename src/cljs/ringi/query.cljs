(ns ringi.query
  (:require [ringi.utils :refer [parse-uuid]]))

(def topics
  '[:find ?e (pull ?e [:topic/id
                       :topic/title
                       :topic/timestamp
                       {:topic/author
                        [:user/id
                         :user/name
                         :user/avatar
                         :user/me]}])
    :where [?e :topic/title]])

(def topics-by-author
  '[:find ?e (pull ?e [:topic/id
                       :topic/title
                       :topic/timestamp
                       {:topic/author
                        [:user/id
                         :user/name
                         :user/avatar
                         :user/me]}])
    :in $ ?u
    :where [?e :topic/author ?u]])

(def topic-by-id
  '[:find ?e
    :in $ ?id
    :where [?e :topic/id ?id]])

(def user-p
  [:user/id
   :user/name
   :user/avatar])

(def vote-p
  [:vote/id
   :vote/value
   {:vote/author [:user/name]}])

(def choice-p
  [:db/id
   :choice/id
   :choice/title
   {:choice/votes vote-p}])

(def topic-p
  [:topic/id
   :topic/title
   :topic/timestamp
   :topic/choices 
   {:topic/author user-p}
   {:topic/choices choice-p}])
  

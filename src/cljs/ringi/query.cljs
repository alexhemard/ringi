(ns ringi.query)

(def topics
  '[:find ?e 
    :where [?e :topic/title]])

(def topics-by-author
  '[:find ?e
    :in $ ?u
    :where [?e :topic/author ?u]])

(def topic-by-id
  '[:find ?e
    :in $ ?id
    :where [?e :topic/id ?id]])

(def vote-by-author
  '[:find ?v
    :in $ ?c ?u
    :where [?c :votes ?v]
           [?v :vote/author ?u]])

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
  

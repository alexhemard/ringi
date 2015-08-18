(ns ringi.query)

(def topics
  '[:find ?e
    :where [?e :topic/title]])

(def topics-by-author
  '[:find [(pull ?e ?selector) ...]
    :in $ ?u ?selector
    :where [?e :topic/author ?a]
           [?a :user/id      ?u]])

(def topic-by-id
  '[:find (pull ?e ?selector) .
    :in $ ?id ?selector
    :where [?e :topic/id ?id]])

(def vote-by-author
  '[:find ?v
    :in $ ?c ?u
    :where [?c :votes ?v]
           [?v :vote/author ?u]])

(def user-p
  [:user/id
   :user/name])

(def vote-p
  [:vote/id
   :vote/value
   {:vote/author user-p}])

(def choice-p
  [:db/id
   :choice/id
   :choice/title
   {:votes vote-p}])


(def topic-p
  [:db/id
   :topic/id
   :topic/title
   :topic/description
   {:topic/author user-p}
   {:topic/choices choice-p}])

(def topics-p
  [:db/id
   :topic/id
   :topic/title
   :topic/description])

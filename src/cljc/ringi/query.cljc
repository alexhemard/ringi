(ns ringi.query)

(def all-topics
  '[:find ?e
    :where [?e :topic/title]])

(def topics-by-author
  '[:find [(pull ?e ?selector) ...]
    :in $ ?user ?selector
    :where [?e :topic/author ?a]
           [?a :user/id      ?user]])

(def topic-by-id
  '[:find (pull ?e ?selector) .
    :in $ ?id ?selector
    :where [?e :topic/id ?id]])

(def vote-by-author
  '[:find ?v
    :in $ ?choice ?user
    :where [?choice :votes ?v]
           [?v :vote/author ?user]])

(def user
  [:user/id
   :user/name])

(def vote
  [:db/id
   :vote/id
   :vote/value
   {:vote/author user}])

(def comment-p
  [:db/id
   :comment/id
   :comment/content
   {:comment/author user}])

(def choice
  [:db/id
   :choice/id
   :choice/title
   {:comments comment}
   {:votes    vote}])

(def topic
  [:db/id
   :topic/id
   :topic/title
   :topic/description
   {:topic/author  user}
   {:topic/choices choice}])

(def topics
  [:db/id
   :topic/id
   :topic/title
   :topic/description])

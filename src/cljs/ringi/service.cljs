(ns ringi.service
  (:require [cljs.core.async :refer [chan pub sub <! put!] :as async]
            [datascript  :as d]
            [goog.events :as events]
            [cognitect.transit :as transit]
            [ringi.mapper :refer [build-mapping*]]
            [ringi.db :refer [bind unbind conn]])
  (:import [goog.net XhrIo]
           goog.net.EventType
           [goog.events EventType])
  (:require-macros
   [cljs.core.async.macros :refer [go go-loop]]
   [ringi.mapper :refer [defmap]]))

(def server-chan (chan 1))

(def p (pub server-chan first))

(defn- format-xhr-response [xhr]
  {:status (.getStatus xhr)
   :headers (js->clj (.getResponseHeaders xhr))
   :body    (try (js->clj (.getResponseJson xhr) :keywordize-keys true)
                 (catch js/Error e nil))})

(defn xhr [{:keys [method url data]}]
  (let [xhr (XhrIo.)
        headers (when-not (string? data) #js {"Content-Type" "application/json" "Accept" "application/json"})
        data (if-not (string? data) (.stringify js/JSON (clj->js data)) data)
        channel (chan)]
    (events/listen xhr goog.net.EventType.SUCCESS
                   (fn [e] (put! channel (format-xhr-response xhr))))
    (events/listen xhr goog.net.EventType.ERROR
      (fn [e] (put! channel (format-xhr-response xhr))))
    (. xhr (send url method data headers))
    channel))

(defn GET
  [url]
  (xhr {:method "GET"
        :url url}))

(defn POST 
  [url data]
  (xhr {:method "POST"
        :url url
        :data data}))

(defn call-server
  ([topic] (call-server topic nil))
  ([topic msg]
   (put! server-chan [topic msg])))

(defmap raw->author [m]
  [:user/id   :id
   :user/name :name])

(defmap raw->vote [m]
  [:vote/id      :id
   :vote/value   :value
   :vote/author {:from :author
                 :fn   raw->author}])

(defmap raw->choice [m]
  [:choice/id     :id
   :choice/title  :title
   :choice/author {:from :author
                   :fn   raw->author}
   :votes         {:from :votes
                   :cardinality :many
                   :fn raw->vote}])

(defmap raw->topic [m]
  [:topic/id    :id
   :topic/title :title
   :topic/choices {:from :choices
                   :cardinality :many
                   :fn   raw->choice}
   :topic/author  {:from :author
                   :fn   raw->author}])

(def persist-chan (chan 1))

(defn start-service! []
  (let [topic-chan        (chan 1)
        topics-chan       (chan 1)
        create-topic-chan (chan 1)
        vote-chan         (chan 1)]

    (sub p :fetch-topic  topic-chan)
    (sub p :fetch-topics topics-chan)
    (sub p :create-topic create-topic-chan)
    (sub p :vote         vote-chan)
    
    (go-loop []
      (let [[topic msg] (<! topics-chan)
            result (GET "/v1/topics")]
        (>! persist-chan (map raw->topic (get-in (<! result) [:body :data])))
        (recur)))

    (go-loop []
      (let [[topic {:keys [topic-id]}] (<! topic-chan)
            result (GET (str "/v1/topics/" topic-id))]
        (>! persist-chan [(raw->topic (get (<! result) :body))])
        (recur)))
    
    (go-loop []
      (let [[topic msg] (<! create-topic-chan)
            result      (POST "/v1/topics" msg)]))
    
    (go-loop []
      (let [[topic {:keys [choice-id vote]}] (<! vote-chan)
            result (POST (str "/v1/choices/" choice-id "/votes") (str "value=" vote))]
        (when (<! result)
          (GET "/v1/"))
        (recur)))
    
    (go-loop []
      (let [tx (<! persist-chan)]
        (d/transact! conn tx))
      (recur))))

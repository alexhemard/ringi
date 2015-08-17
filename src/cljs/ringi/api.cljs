(ns ringi.api
  (:require [cljs.core.async :refer [chan pub sub <! put!] :as async]
            [datascript  :as d]
            [goog.events :as events]
            [cognitect.transit :as transit]
            [datascript :as d]
            [clojure.string :refer [join]]
            [secretary.core :as secretary]
            [ringi.mapper :refer [build-mapping*]])
  (:import [goog.net XhrIo]
           goog.net.EventType
           [goog.events EventType])
  (:require-macros
   [cljs.core.async.macros :refer [go go-loop]]
   [ringi.mapper :refer [defmap]]))

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

(def api-root "/v1")

(def topics-path (str api-root "/topics"))

(defn topic-path [topic-id]
  (join "/" [topics-path topic-id]))

(defn choices-path [topic-id]
  (join "/" [topics-path "choices"]))

(defn choice-path [choice-id]
  (join "/" [api-root "choices" choice-id]))

(defn votes-path [choice-id]
  (join "/" [(choice-path choice-id) "votes"]))

(defmulti handle
  (fn [method args state] method))

(defmethod handle :get-topics
  [method args state]
  (go
    (let [{:keys [comms]} @state
          persist-ch (:persist comms)
          result (GET topics-path)
          data (mapv raw->topic (get-in (<! result) [:body :data]))]
      (if (seq data)
        (>! persist-ch data)))))

(defmethod handle :get-topic
  [method id state]
  (go
    (let [{:keys [comms]} @state
          persist-ch (:persist comms)
          result (GET (topic-path id))
          data (raw->topic (get (<! result) :body))]
      (if (seq data)
        (>! persist-ch [data])))))

(defmethod handle :create-topic
  [method args state]
  (go
    (let [{:keys [data]}  args
          {:keys [history comms]} @state
          persist-ch (:persist comms)
          nav-ch (:nav comms)          
          result (<! (POST topics-path data))
          data (get-in result [:body :data])
          location (get-in result [:headers "Location"])
          [_ id] (re-find #"/v1/topics/(\S+)" location)
          new-topic (str "/t/" id)]
      (.setToken history new-topic))))

(defmethod handle :create-choice
  [method args state]
  (go
    (let [{:keys [topic-id data]}  args
          {:keys [comms]} @state
          persist-ch (:persist comms)
          result (<! (POST choices-path data))]
      

      )))

(defmethod handle :vote
  [method args state]
  (go
    (let [{:keys [choice-id data]}  args
          {:keys [comms]} @state
          persist-ch (:persist comms)
          result (<! (POST (votes-path choice-id) data))]
      

      )))

(defmethod handle :default
  [method args state])

(defn call
  ([ch method] (call ch method {}))
  ([ch method args]
   (put! ch [method args])))

(comment
  (defn start-service! []
    (let [topic-chan        (chan 1)
          topics-chan       (chan 1)
          create-topic-chan (chan 1)
          vote-chan         (chan 1)]
      
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
        (recur)))))

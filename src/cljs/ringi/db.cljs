(ns ringi.db
  (:require [reagent.core :as r :refer [atom]]
            [datascript :as d]
            [ringi.utils :refer [guid]]))

(defn bind
  [conn q & q-args]
  (let [state (atom nil)
        k     (guid)]
    (reset! state (apply d/q q @conn q-args))
    (d/listen! conn k (fn [tx-report]
                        (let [changes (apply d/q q (:tx-data tx-report) q-args)]
                          (when (not-empty changes) ;; Only update if query results actually changed
                            (reset! state (apply d/q q (:db-after tx-report) q-args))))))
    (set! (.-__key state) k)
    state))

(defn unbind
  [conn state]
  (d/unlisten! conn (.-__key state)))

(def schema
  {;; users
   
   :user/id           {:db/unique :db.unique/identity}
   :user/name         {:db/unique :db.unique/identity}
   :user/avatar       {}
   :user/me           {}

   ;; topics
   
   :topic/id          {:db/unique :db.unique/identity}
   :topic/title       {}
   :topic/description {}
   :topic/timestamp   {}
   :topic/author      {:db/valueType    :db.type/ref
                       :db/cardinality  :db.cardinality/one}
   :topic/choices     {:db/valueType    :db.type/ref
                       :db/cardinality  :db.cardinality/many
                       :db/isComponent  true}

   ;; items
   
   :choice/id         {:db/unique :db.unique/identity}
   :choice/title      {}
   :choice/author     {:db/valueType :db.type/ref
                       :db/cardinality :db.cardinality/one}
   :choice/timestamp  {}
   :choice/votes      {:db/valueType    :db.type/ref
                       :db/cardinality  :db.cardinality/many
                       :db/isComponent  true}

   ;; comments
   
   :comments   {:db/valueType    :db.type/ref
                :db/cardinality  :db.cardinality/many
                :db/isComponent  true}
   
   ;; votes
   
   :vote.value        {}
   :vote/author       {:db/valueType :db.type/ref}

   ;; comments
   
   :comment/body      {}
   :comment/author    {:db/valueType :db.type/ref}})

(defn str-uuid []
  (str (d/squuid)))

(def fixtures
  [{:db/id -1
    :user/id (str-uuid)
    :user/name "alexhemard"}
   {:db/id -2
    :user/id (str-uuid)
    :user/name "mshwery"}
   {:db/id -3
    :user/id (str-uuid)
    :user/name "csampson"}
   {:db/id -4
    :user/id (str-uuid)
    :user/name "jwheeler"}
   {:db/id -5
    :user/id (str-uuid)
    :user/name "feedjoelpie"}
   {:db/id -6
    :user/id (str-uuid)
    :user/name "notjoeellis"}
   {:topic/id (str-uuid)
    :topic/title "What should happen to VoteIt?"
    :topic/author {:db/id -1}
    :topic/choices [{:choice/id (str-uuid)
                     :choice/title "let it burn..."
                     :choice/author -6
                     :choice/votes [{:vote/value :no
                                     :vote/author {:db/id -1}}
                                    {:vote/value :ok
                                     :vote/author {:db/id -3}}
                                    {:vote/value :no
                                     :vote/author {:db/id -4}}
                                    {:vote/value :ok
                                     :vote/author {:db/id -5}}
                                    {:vote/value :no
                                     :vote/author {:db/id -6}}]}                    
                    {:choice/id (str-uuid)
                     :choice/title "Bring it back. I feel dead inside"
                     :choice/author -1
                     :choice/votes [{:vote/value :no
                                     :vote/author {:db/id -2}}
                                    {:vote/value :yes
                                     :vote/author {:db/id -1}}
                                    {:vote/value :ok
                                     :vote/author {:db/id -3}}
                                    {:vote/value :ok
                                     :vote/author {:db/id -4}}
                                    {:vote/value :yes
                                     :vote/author {:db/id -5}}
                                    {:vote/value :yes
                                     :vote/author {:db/id -6}}]}
                    {:choice/id (str-uuid)
                     :choice/title "tybg for voteits."
                     :choice/author -3
                     :choice/votes [{:vote/value :no
                                     :vote/author {:db/id -2}}
                                    {:vote/value :yes
                                     :vote/author {:db/id -4}}
                                    {:vote/value :ok
                                     :vote/author {:db/id -5}}
                                    {:vote/value :ok
                                     :vote/author {:db/id -6}}]
                     :comments [{:comment/author -5
                                 :comment/body "what is a 'bg'"}]}]
    :comments [{:comment/author -1
                :comment/body "what has this world come to?"}]}
   {:topic/id (str-uuid)
    :topic/title "What should i do 4 my birthday."
    :topic/author {:db/id -3}
    :topic/choices [{:choice/id (str-uuid)
                     :choice/title "bumper cars"
                     :choice/author -2
                     :choice/votes [{:vote/value :no
                                     :vote/author {:db/id -1}}
                                    {:vote/value :ok
                                     :vote/author {:db/id -3}}]}                    
                    {:choice/id (str-uuid)
                     :choice/title "laser tag"
                     :choice/author -5
                     :choice/votes [{:vote/value :no
                                     :vote/author {:db/id -2}}
                                    {:vote/value :yes
                                     :vote/author {:db/id -5}}
                                    {:vote/value :yes
                                     :vote/author {:db/id -6}}]}]}
   {:topic/id (str-uuid)
    :topic/title "I'm not sure what to bring for dinner..."
    :topic/author {:db/id -2}
    :topic/choices [{:choice/id (str-uuid)
                     :choice/title "pizza"
                     :choice/author -6
                     :choice/votes [{:vote/value :ok
                                     :vote/author {:db/id -1}}
                                    {:vote/value :ok
                                     :vote/author {:db/id -5}}
                                    {:vote/value :yes
                                     :vote/author {:db/id -6}}]}                    
                    {:choice/id (str-uuid)
                     :choice/title "don't bring anything. you're perfect :1)"
                     :choice/author -1
                     :choice/votes [{:vote/value :no
                                     :vote/author {:db/id -2}}
                                    {:vote/value :no
                                     :vote/author {:db/id -6}}]}
                    {:choice/id (str-uuid)
                     :choice/title "bring vote it back from the grave."
                     :choice/author -3
                     :choice/votes [{:vote/value :yes
                                     :vote/author {:db/id -2}}
                                    {:vote/value :yes
                                     :vote/author {:db/id -4}}
                                    {:vote/value :ok
                                     :vote/author {:db/id -5}}
                                    {:vote/value :ok
                                     :vote/author {:db/id -6}}]}
                    {:choice/id (str-uuid)
                     :choice/title "a great attitude."
                     :choice/author -3
                     :choice/votes [{:vote/value :yes
                                     :vote/author {:db/id -2}}
                                    {:vote/value :yes
                                     :vote/author {:db/id -4}}
                                    {:vote/value :yes
                                     :vote/author {:db/id -1}}
                                    {:vote/value :no
                                     :vote/author {:db/id -3}}
                                    {:vote/value :ok
                                     :vote/author {:db/id -6}}]}
                    ]}])

(def conn
  (d/create-conn schema))

(defn init-db! [] (d/transact! conn fixtures))

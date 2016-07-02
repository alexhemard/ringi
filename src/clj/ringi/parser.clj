(ns ringi.parser
  (:refer-clojure :exclude [read])
  (:require [datomic.api :as d]
            [om.next.server :as om]
            [datomic.api :as d]))

(defn topics
  ([db]
   (topics db nil))
  ([db selector]
   (let [q '[:find [(pull ?eid selector) ...]
             :in $ selector
             :where
             [?eid :topics/guid]]]
     (d/q q db (or selector '[*])))))

(defmulti readf om/dispatch)

(defmethod readf :default
  [_ k _]
  {:value {:error (str "No handler for read key " k)}})

(defmethod readf :topics
  [{:keys [datomic selector]} k _]
  (let [conn (:conn datomic)]
    {:value (topics (d/db conn) selector)}))

;; =============================================================================
;; Mutations

(defmulti mutatef om/dispatch)

(defmethod mutatef :default
  [_ k _]
  {:value {:error (str "No handler for mutation key " k)}})

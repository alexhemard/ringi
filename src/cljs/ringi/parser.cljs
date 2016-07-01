(ns ringi.parser
  (:require [om.next :as om]
            [om.next.impl.parser :as p]))

(defmulti read om/dispatch)

(defmethod read :default
  [{:keys [state]} k _]
  (let [st @state]
    (if (contains? st k)
      {:value (get st k)})))

(defmulti mutate om/dispatch)

(defmethod mutate :default
  [_ _ _] {:value []})

(ns ringi.parser
  (:require [om.next :as om]
            [om.next.impl.parser :as p]
            [ringi.navigation :refer [navigate-to]]))

(defn- join? [x]
  (let [x (if (seq? x) (first x) x)]
    (map? x)))

(defn parameterize-joins [query params]
  (-> query
    p/query->ast
    (update :children
      (partial mapv (fn [ast] (if (= :join (:type ast)) (update ast :params merge params) ast))))
    (update :params merge params)
    p/ast->expr))

(defmulti read om/dispatch)

(defmethod read :default
  [{:keys [state]} k _]
  (let [st @state]
    (if (contains? st k)
      {:value (get st k)})))

(defmethod read :current-page
  [{:keys [target parser state ast query] :as env} k params]
  (let [st @state]
    (if target
      (let [query (parameterize-joins query params)
            query (parser env query target)
            query (filterv #(pos? (count %)) query)]
        (println "query" query)
        (when-not (empty? query)
          {target (assoc ast :query query)}))
      (if (get st k)
        {:value (om/db->tree query (get st k) st)}))))

(defmethod read :topics/list
  [{:keys [state query]} k params]
  ; todo
  {:value []})

(defmulti mutate om/dispatch)

(defmethod mutate :default
  [_ _ _] {:value []})

(defmethod mutate 'app/navigate
  [{:keys [state] :as env} k {:keys [handler params]}]
  {:value {:keys [:current-page]}
   :action (fn []
             (let [params (or params {})]
               (swap! state (fn [st]
                              (-> st
                                (assoc :handler handler)
                                (dissoc :current-page)
                                (navigate-to handler params))))))})

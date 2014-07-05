(ns ringi.models.vote
    (:require [clojure.java.jdbc   :as jdbc]
              [honeysql.core       :as sql]
              [honeysql.helpers    :refer :all]
              [ringi.models.choice :refer [with-comments-for-choices]]
              [ringi.util :refer [reorder-map]]))


(defn comment->json [row]
  (-> row
      (reorder-map [:id
                    :author
                    :comment
                    :created_at])))

(defn decision->json [row]
  (-> row
      (reorder-map [:id
                    :author
                    :value])))

(defn choice->json [row]
  (-> row
      (reorder-map [:id
                    :author
                    :title
                    :decisions
                    :comments])
      (update-in [:decisions] #(map decision->json %))
      (update-in [:comments]  #(map comment->json %))))

(defn vote->json [row]
  (-> row
      (reorder-map [:id
                    :author
                    :title
                    :description
                    :created_at
                    :updated_at
                    :comments
                    :choices])
      (update-in [:choices]  #(map choice->json %))
      (update-in [:comments] #(map comment->json %))))

(defn collect-prefixed [m prefix]
  (let [match-fn (fn [k] (second (re-find (re-pattern (str  "^" prefix "(.+)")) k)))]
    (reduce (fn [m [k v]] (if-let [key (match-fn (name k))] (assoc m (keyword key) v) m)) {} m)))

(defn merge-author [m]
  (clojure.walk/prewalk (fn [form]
                          (if (map? form)
                            (let [author (collect-prefixed form "author_")]
                              (if (empty? author)
                                form
                                (assoc form :author author)))
                            form))
                        m))

(defn- paginate [sql page per-page]
  (-> sql
      (offset (* (- page 1) per-page))
      (limit per-page)))

(defn- with-comments-for-votes [votes db]
  (if (not (empty? votes))
    (let [ids (map :id votes)
          comments (jdbc/query
                    db
                    (-> (select :comments.*
                                [:comments_votes.vote_id :vote_id]
                                [:users.name :author_name])
                        (from :comments)
                        (join :comments_votes [:= :comments_votes.comment_id :comments.id]
                              :users [:= :comments.author_id :users.id])
                        (where [:in :comments_votes.vote_id ids])
                        (sql/format :quoting :ansi)))
          comments (group-by :vote_id comments)
          votes (map (fn [v] (assoc v :comments (or (get comments (:id v)) []))) votes)]
      votes)
    votes))

(defn- with-decisions [choices db]
  (if (not (empty? choices))
    (let [ids (map :id choices)
          decisions (jdbc/query
                     db
                     (-> (select :decisions.*
                                 [:users.name :author_name])
                         (from :decisions)
                         (join :choices [:= :choices.id :decisions.choice_id]
                               :users [:= :decisions.author_id :users.id])
                         (where [:in :decisions.choice_id ids])
                         (sql/format :quoting :ansi)))
          decisions (group-by :choice_id decisions)
          choices (map (fn [c] (assoc c :decisions (or (get decisions (:id c)) []))) choices)]
      choices)
    choices))

(defn- with-choices [votes db]
  (if (not (empty? votes))
    (let [ids (map :id votes)
          choices (-> (jdbc/query
                       db
                       (-> (select :choices.*
                                   [:users.name :author_name])
                           (from :choices)
                           (join :votes [:= :votes.id :choices.vote_id]
                                 :users [:= :choices.author_id :users.id])
                           (where [:in :choices.vote_id ids])
                           (sql/format :quoting :ansi)))
                      (with-decisions db)
                      (with-comments-for-choices db))
          choices (group-by :vote_id choices)
          votes (map (fn [v] (assoc v :choices (or (map #(dissoc % :vote_id) (get choices (:id v))) []))) votes)]
      votes)
    votes))

(defn- fetch-all [db query]
  (-> (jdbc/query
       db (-> query
              (select :votes.*
                      [:users.name :author_name])
              (from :votes)
              (join :users [:= :votes.author_id :users.id])
              (sql/format :quoting :ansi)))
      (with-choices db)
      (with-comments-for-votes db)
      (merge-author)
      (#(map vote->json %))))

(defn fetch-all-for-user [db user-id page per-page]
  (let [page     (or page     1)
        per-page (or per-page 25)]
    (fetch-all
     db
     (-> (where [:= :votes.author_id user-id])
         (paginate page per-page)))))

(defn fetch [db id]
  (first
   (fetch-all
    db
    (-> (where [:= :votes.id id])))))

(ns ringi.models.choice
    (:require [clojure.java.jdbc :as jdbc]
              [honeysql.core     :as sql]
              [honeysql.helpers  :refer :all]
              [ringi.util :refer [reorder-map]]))

(defn with-comments-for-choices [choices db]
  (if (not (empty? choices))
    (let [ids (map :id choices)
          comments (jdbc/query
                    db
                    (-> (select :comments.*
                                [:comments_choices.choice_id :choice_id]
                                [:users.name :author_name])
                        (from :comments)
                        (join :comments_choices [:= :comments_choices.comment_id :comments.id]
                              :users [:= :comments.author_id :users.id])
                        (where [:in :comments_choices.choice_id ids])
                        (sql/format :quoting :ansi)))
          comments (group-by :choice_id comments)
          choices (map (fn [c] (assoc c :comments (or (get comments (:id c)) []))) choices)]
      choices)
    choices))

(defn- fetch-all [db query]
  (-> (jdbc/query
       db (-> query
              (select :choices.*
                      [:users.name :author_name])
              (from :choices)
              (join :users [:= :choices.author_id :users.id])
              (sql/format :quoting :ansi)))
      (with-comments-for-choices db)
      (merge-author)
      (#(map vote->json %))))

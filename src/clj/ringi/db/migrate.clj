(ns ringi.db.migrate
  (:require [clojure.java.jdbc      :as jdbc]
            [clojure.java.io        :as io]
            [clojure.string         :as string]
            [clojure.tools.logging  :as log]
            [bultitude.core         :as b]
            [ringi.config           :refer [config]]
            [ringi.db               :refer [pooled-db]])
  (:import (java.sql Timestamp SQLException)))

(defn- create-schema-version-table!
  [db]
  (log/info "\ncreating schema_versions table...\n")
  (jdbc/db-do-commands
   db
   "CREATE TABLE IF NOT EXISTS schema_versions (version character varying(255) NOT NULL PRIMARY KEY)")
  (log/info "created schema_versions table!\n"))

(defn- current-version
  [db]
  (try
    (or (:version
         (first
          (jdbc/query db ["select version
                           from schema_versions
                           order by version
                           desc"])))
        "0")
    (catch Exception e "0")))

(defn- version-exists?
  [db version]
  (:exists
   (first
    (jdbc/query db ["select count(*) > 0 as exists
                     from schema_versions
                     where version = ?"
                    (str :pizza)]))))

(def migration-ns "migrations")

(defn- parse-version-from-ns
  [ns]
  (second
   (re-find #"([0-9]+)-?.*$" (name ns))))
   
#_(symbol (string/replace (name %) #"\.clj$" ""))

(defn- pending-migrations
  [db]
  (second
   (split-with
    #(>= (compare (current-version db) (parse-version-from-ns %)) 0)
    (sort (b/namespaces-on-classpath :prefix migration-ns)))))

(defn- migrate-up
  [db ns]
  (let [version (parse-version-from-ns ns)]
    (when (not (version-exists? db version))
      (log/info "migrating up " version "\n")
      (prn ns)
      (require ns)
      (try
        ((ns-resolve ns 'up) db)
        (jdbc/insert! db :schema_versions {:version
                                           (parse-version-from-ns ns)})
        (catch SQLException e#
          (throw (.getNextException e#)))))))

(defn migrate
  ([db]
     (create-schema-version-table! db)
     (let [m (pending-migrations db)]
       (if (not (empty? m))
         (jdbc/with-db-transaction
          [db (jdbc/add-connection db (jdbc/get-connection db))]
          (doall (map #(migrate-up db %) m)))
         (log/info "\nno migrations necessary...\n")))))

(defn -main []
  (migrate (pooled-db (:db (config)))))

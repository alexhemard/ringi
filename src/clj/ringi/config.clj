(ns ringi.config
  (:require [ringi.config.db      :as config-db]
            [clojure.tools.cli        :refer [cli]]
            [clojure.tools.reader.edn :as edn])
  (:import [java.net URI]))

(defn remove-nil-vals [m]
  (into {} (remove #(nil? (val %)) m)))

(def default-config
  {:http {:port 8080
          :ip "0.0.0.0"}
   :db {:classname   "org.postgresql.Driver"
        :subprotocol "postgresql"
        :subname     "//localhost/ringi"
        :user        "postgres"}
   })

(defn- getenv [key & default]
  (or (System/getenv key) default))

(defn- parse-config-file [f]
  (try
    (edn/read-string (slurp f))
    (catch Exception e {})))

(defn environment [] (getenv "RINGI_ENV" "development"))

(defn development? []
  (= (environment) "development"))

(defn production? []
  (= (environment) "production"))

(defn config [& {:keys [config-file env]
                 :or   {config-file "config.edn"
                        env         "development"}}]
  (-> default-config
      (merge (get (parse-config-file "config.edn") env))
      (cond-> (getenv "DATABASE_URL")
              (assoc :db (config-db/parse-jdbc-url (getenv "DATABASE_URL"))))))

(defn- parse-args [args defaults]
  (cli args
    ["-h" "--help" "Show this help text and exit" :flag true]
    ["-f" "Read configuration map from a file" :name :config-file]
    ["-p" "--port" "Port to listen on for web requests"
     :parse-fn #(Integer/parseInt %) :default (:port defaults)]
    ["-b" "--bind" "Address to bind to for web requests"
     :default (:bind defaults)]
    ["--db" "Database URL like sqlite:data/db"]))

(defn- parse-config [args]
  (let [[arg-opts args banner] (parse-args args {})
        arg-opts (remove-nil-vals arg-opts)
        opts (if-let [f (:config-file arg-opts)]
               (merge (parse-config-file f) arg-opts)
               arg-opts)]
    [opts args banner]))

(defn config-cli [args]
  (let [[options args banner] (parse-config args)
        options (merge options (config))]
    (when (:help options)
      (println "Ringi starting...")
      (System/exit 0))
    options))

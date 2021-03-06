(ns ringi.config
  (:require [ringi.util :refer [parse-int]]
            [clojure.tools.reader.edn :as edn]))

(defn getenv [key & [default]]
  (or (System/getenv key) default))

(def default-config
  {:env (getenv "RINGI_ENV" "development")
   :http {:port (parse-int (getenv "PORT" 5000))
          :host "0.0.0.0"}
   :datomic {:uri "datomic:mem://ringi" }})

; "datomic:sql://ringi?jdbc:postgresql://localhost:5432/datomic?user=datomic&password=datomic"

(defn config [& {:keys [config-file]
                 :or   {config-file "config/development.edn"}}]
  (-> default-config
      (merge (edn/read-string (slurp (clojure.java.io/resource config-file))))))

(ns ringi.test.helper
  (:require [clojure.java.io :as io]
            [ringi.system  :refer [system]]
            [ringi.config  :refer [config]]
            [ringi.datomic :refer [create-datomic]]
            [ringi.app     :refer [create-app]]
            [datomic.api   :as     d]
            [clojure.test  :refer :all]
            [com.stuartsierra.component :as component]))

(def ^{:dynamic true} *system* nil)

(def fixtures
  (-> (io/resource "test/fixtures.edn")
      slurp
      read-string))

(def test-config
  {:env "test"
   :datomic {:uri "datomic:mem://ringi-test" }})

(defn system-fixture [f]
  (let [system (component/start-system (system test-config) [:app :datomic])]
    (binding [*system* system]
      (f))
    (component/stop-system system)))

(defn datomic-fixture [f]
  (let [{:keys [conn uri]} (:datomic *system*)
        tx (vec (mapcat #(fixtures %) [:users :topics :choices :votes]))]
    @(d/transact conn tx)
    (f)
    (d/delete-database uri)))

(def default-fixture (compose-fixtures system-fixture datomic-fixture))



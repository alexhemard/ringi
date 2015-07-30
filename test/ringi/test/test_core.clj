(ns ringi.test.test-core
  (:require [clojure.test :refer :all]
            [ringi.system :refer [system]]
            [ringi.config :refer [config]]))

(defn default-fixture [f]
  (let [app (system)]
    (.start app)
    (f)
    (.stop app)))

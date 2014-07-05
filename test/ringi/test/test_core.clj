(ns ringi.test.test-core
  (:require [clojure.test :refer :all]
            [ringi.system :as system]
            [ringi.config :refer [config]]))

(defn default-fixture [f]
  (let [system (system/system)])
  (.start system)
  (f)
  (.stop system))

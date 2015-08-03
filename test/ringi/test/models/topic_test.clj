(ns ringi.test.models.topic-test
  (:require [clojure.test       :refer :all]
            [ringi.models.topic :as     topic]
            [ringi.test.helper  :refer [fixtures default-fixture *system*]]))

(use-fixtures :each default-fixture)

(deftest find
  (let [conn   (get-in *system* [:datomic :conn])
        topicf (get-in fixtures [:topics 0])
        uid    (:topic/uid topicf)
        topic  (topic/fetch conn uid)]
    (is (= (:topic/title topicf)       (:topic/title topic)))
    (is (= (:topic/description topicf) (:topic/description topic)))))

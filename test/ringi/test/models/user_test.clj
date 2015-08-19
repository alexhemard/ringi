(ns ringi.test.models.user-test
  (:require [clojure.test      :refer :all]
            [ringi.models.user :as     user]
            [ringi.test.helper :refer [fixtures default-fixture *system*]]))

(use-fixtures :each default-fixture)

(deftest get-user
  (let [conn  (get-in *system* [:datomic :conn])
        userf (get-in fixtures [:users 0])
        user  (user/fetch conn (:user/uid userf))]
    (is (= (:user/uid userf)      (:user/uid user)))
    (is (= (:user/name userf)     (:user/name user)))
    (is (= (:user/email userf)    (:user/email user)))
    (is (= (:user/password userf) (:user/password user)))))

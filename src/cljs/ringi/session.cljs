(ns ringi.session
  (:require [ringi.dom :as dom]))

(defn init-current-user! [state]
  (let [name (dom/q "meta[user_name]")
        id   (dom/q "meta[user_uid]")]
    (if (and name id)
      (swap! state assoc :current-user
             {:id   (dom/get-attribute id   "user_uid")
              :name (dom/get-attribute name "user_name")}))))

(defn init! [state]
  (init-current-user! state))

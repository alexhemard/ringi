(ns ringi.session
  (:require [reagent.core :as r]
            [ringi.dom    :as dom]))

(def state (r/atom nil))

(defn puts! [k v]
  (swap! state assoc k v))

(defn init-current-user! []
  (let [name (dom/q "meta[user_name]")
        id   (dom/q "meta[user_uid]")]
    (if (and name id)
      (puts! :current-user {:id   (dom/get-attribute id   "user_uid")
                            :name (dom/get-attribute name "user_name")}))))

(defn current-user []
  (:current-user @state))

(defn init! []
  (init-current-user!))

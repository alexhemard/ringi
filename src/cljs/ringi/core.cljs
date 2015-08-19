(ns ringi.core
    (:require-macros [cljs.core.async.macros :refer [go-loop alt!]])
    (:require [cljs.core.async :as async :refer [chan mult tap]]
              [goog.events :as events]
              [goog.dom :as gdom]
              [goog.object :as gobj]
              [goog.history.EventType :as EventType]
              [om.core :as om :include-macros true]
              [secretary.core :as secretary]
              [goog.history.EventType :as EventType]
              [ringi.api :as api]
              [ringi.db :as db]
              [ringi.component :refer [app] :as c]
              [ringi.routes :refer [init-routes!] :as routes]
              [ringi.dom :as dom])
    (:import [goog.history Html5History]
             [goog Uri]))

(defn current-user []
  (let [name (dom/q "meta[user_name]")
        id   (dom/q "meta[user_uid]")]
    (when (and name id)
      {:id   (dom/get-attribute id   "user_uid")
       :name (dom/get-attribute name "user_name")})))

(defn app-target [] (.getElementById js/document "root"))

(def navigation-ch  (chan))
(def api-ch         (chan))
(def persistence-ch (chan))

(def navigation-mult  (mult navigation-ch))
(def api-mult         (mult api-ch))
(def persistence-mult (mult persistence-ch))

;; history

(def history
  (doto (Html5History.)
    (.setUseFragment false)
    (.setPathPrefix  "")
    (.setEnabled     true)))

(defn init-history! []
  (events/listen history EventType/NAVIGATE
    (fn [e]
      (when (secretary/locate-route (.-token e))
        (secretary/dispatch! (.-token e)))))

  (events/listen js/document "click"
    (fn [e]
      (let [target (.-target e)]
        (when (= (.-tagName target) "A")
          (let [path (.getPath (.parse Uri (.-href target)))]
            (when (secretary/locate-route path)
              (.preventDefault e)
              (.setToken history path)))))))

  (secretary/dispatch! (-> js/window .-location .-pathname)))

(defn app-state []
  (atom {:current-user (current-user)
         :conn         db/conn
         :history      history
         :comms        {:nav     navigation-ch
                        :api     api-ch
                        :persist persistence-ch}}))

(defn handle-persist [tx state]
  (db/handle tx state))

(defn handle-api [method args state]
  (api/handle method args state))

(defn handle-nav [path params state]
  (swap! state (fn [s] (-> s
                        (assoc :current-page path)
                        (assoc :params params))))
  (routes/handle path params state))

(defn main [state]
  (let [nav-tap     (chan)
        api-tap     (chan)
        persist-tap (chan)]

    (async/tap api-mult         api-tap)
    (async/tap navigation-mult  nav-tap)
    (async/tap persistence-mult persist-tap)

    (go-loop []
      (alt!
        api-tap     ([[method args]] (handle-api method args state))
        nav-tap     ([[path params]] (handle-nav path params state))
        persist-tap ([tx]            (handle-persist tx state)))
        (recur))

    (om/root app state {:target (app-target)
                        :shared {:conn  db/conn
                                 :comms (:comms @state)
                                 :current-user  (:current-user @state)}})))

(defn init! []
  (let [state (app-state)]
    (init-routes! state)
    (init-history!)
    (main state)))

(init!)

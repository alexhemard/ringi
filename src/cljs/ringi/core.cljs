(ns ringi.core
    (:require-macros [cljs.core.async.macros :refer []])
    (:require [goog.events :as events]
              [goog.dom :as gdom]
              [goog.object :as gobj]
              [goog.history.EventType :as EventType]
              [om.core :as om :include-macros true]
              [secretary.core :as secretary :refer-macros [defroute]]
              [goog.history.EventType :as EventType]
              [ringi.session  :as session]
              [ringi.service :refer [call-server start-service!]]
              [ringi.component :as c])
    (:import [goog.history Html5History]
             [goog Uri]))

(def app-state (atom nil))

;; history

(defn init-history! []
  (let [history (Html5History.)]
    (events/listen
     history
     EventType/NAVIGATE
     (fn [e]
       (when (secretary/locate-route (.-token e))
         (secretary/dispatch! (.-token e)))))

    (.setUseFragment history false)
    (.setPathPrefix  history "")
    (.setEnabled     history true)

    (events/listen
     js/document
     "click"
     (fn [e]
       (let [target (.-target e)]
         (when (= (.-tagName target) "A")
           (let [path (.getPath (.parse Uri (.-href target)))]
             (when (secretary/locate-route path)
               (.preventDefault e)
               (.setToken history path)))))))
    
    (secretary/dispatch! (-> js/window .-location .-pathname))))

(def target (.getElementById js/document "root"))

;; Routes

(defroute "/" []
  (om/root c/index app-state {:target target}))

(defroute "/register" []
  (om/root c/index app-state {:target target}))

(defroute "/login" []
  (om/root c/index app-state {:target target}))

(defroute "/t/new" []
  (om/root c/index app-state {:target target}))

(defroute "/t/:id" [id]
  (swap! app-state assoc :topic-id id)
  (om/root c/index app-state {:target target}))

(defn main []
  (session/init! app-state)
  (init-history!)
  (start-service!))

(main)



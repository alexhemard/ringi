(ns ringi.core
    (:require-macros [cljs.core.async.macros :refer []])
    (:require [goog.events :as events]
              [goog.dom :as gdom]
              [goog.object :as gobj]
              [goog.history.EventType :as EventType]
              [cljs.core.async :as async]
              [cljs-http.client :as http]
              [reagent.core :as r :refer [atom]]
              [secretary.core :as secretary :refer-macros [defroute]]
              [goog.history.EventType :as EventType]
              [ringi.session :refer [puts! current-user] :as session]
              [ringi.service :refer [call-server start-service!]]
              [ringi.component :as c])
    (:import [goog.history Html5History]
             [goog Uri]))

;; Routes

(defn current-page-render []
  (:current-page @session/state))

(defn current-page []
  (r/create-class {:reagent-render (fn [] [c/page (current-page-render)])}))

(defroute "/" []
  (puts! :current-page [c/topics]))

(defroute "/register" []
  (puts! :current-page [c/register]))

(defroute "/login" []
  (puts! :current-page [c/login]))

(defroute "/t/new" []
  (puts! :current-page [c/topics-new]))

(defroute "/t/:id" [id]
  (puts! :current-page [c/topic-show id]))

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

(defn main []
  (session/init!)
  (init-history!)
  (start-service!)
  (r/render-component [current-page] (gdom/getElement "root")))

(main)



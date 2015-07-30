(ns ringi.core
    (:require-macros [cljs.core.async.macros :refer [go alt!]])
    (:require [goog.events :as events]
              [goog.dom :as gdom]
              [goog.object :as gobj]
              [goog.history.EventType :as EventType]
              [cljs.core.async :as async]
              [cljs-http.client :as http]
              [reagent.core :as r :refer [atom]]
              [secretary.core :as secretary :refer-macros [defroute]]
              [goog.history.EventType :as EventType]
              [ringi.component :as c]
              [ringi.db :refer [init-db!]]
              [ringi.utils :refer [guid]])
    (:import [goog.history Html5History]
             [goog Uri]))

(def app-state (r/atom nil))

(defn put! [k v]
  (swap! app-state assoc k v))

;; Routes

(defn current-page-render []
   (:current-page @app-state))

(defn current-page []
  (r/create-class {:reagent-render (fn [] [c/page (current-page-render)])}))

(defroute "/" []
  (put! :current-page [c/topics]))

(defroute "/register" []
  (put! :current-page [c/register]))

(defroute "/login" []
  (put! :current-page [c/login]))

(defroute "/t/:id" [id]
  (put! :current-page [c/topic-show id]))

(defroute "/t/new" []
  (put! :current-page [c/topics-new]))

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
               (.log js/console path)
               (.setToken history path)))))))
    
    (secretary/dispatch! (-> js/window .-location .-pathname))))

(defn main []
  (init-history!)
  (init-db!)
  (r/render-component [current-page] (gdom/getElement "root")))

(main)



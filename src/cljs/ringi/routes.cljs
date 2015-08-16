(ns ringi.routes
  (:require [cljs.core.async :as async :refer [put!]]
            [secretary.core :as secretary :refer-macros [defroute]]
            [ringi.api :as api]
            [ringi.component :as c]))

(defmulti handle
  (fn [path params state] path))

(defmethod handle :index
  [path params state]
  (let [api-ch (get-in @state [:comms :api])]
    (api/call api-ch :get-topics)))

(defmethod handle :show-topic
  [path params state]
  (.log js/console "show-topicc"))

(defmethod handle :default
  [path params state]
  (.log js/console "whatever"))

(defn navigate
  ([ch path]
   (navigate ch path {}))
  ([ch path args]
   (put! ch [path args])))

(defn init-routes! [state]
  (let [nav-ch (get-in @state [:comms :nav])]

    (defroute "/" []
      (navigate nav-ch :index))
    
    (defroute "/register" []
      (navigate nav-ch :register))
    
    (defroute "/login" []
      (navigate nav-ch :login))
    
    (defroute "/t/new" []
      (navigate nav-ch :new-topic))
    
    (defroute "/t/:id" [id]
      (navigate nav-ch :show-topic {:id id}))
    
    (defroute "*" []
      (navigate nav-ch :not-found))))

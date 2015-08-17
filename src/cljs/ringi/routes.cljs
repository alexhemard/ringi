(ns ringi.routes
  (:require [cljs.core.async :as async :refer [put!]]
            [secretary.core :as secretary :refer-macros [defroute]]
            [ringi.api :as api]
            [ringi.component :as c]))

(defmulti handle
  (fn [path params state] path))

(defmethod handle :dashboard
  [path params state]
  (let [api-ch (get-in @state [:comms :api])]
    (api/call api-ch :get-topics)))

(defmethod handle :show-topic
  [path {:keys [id]} state]
  (let [api-ch (get-in @state [:comms :api])]
    (api/call api-ch :get-topic id)))

(defmethod handle :default
  [path params state])

(defn navigate
  ([ch path]
   (navigate ch path {}))
  ([ch path args]
   (put! ch [path args])))

(defn init-routes! [state]
  (let [nav-ch (get-in @state [:comms :nav])]

    (defroute "/" []
      (if (:current-user @state)
        (navigate nav-ch :dashboard)
        (navigate nav-ch :index)))
    
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

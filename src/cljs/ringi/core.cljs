(ns ringi.core
  (:require [om.next      :as om :refer-macros [defui]]
            [goog.dom     :as gdom]
            [ringi.util   :refer [send-remotes]]
            [ringi.parser :as p]
            [ringi.routes :refer [routers]]
            [ringi.router :refer [start-router]]))

(defui Ringi
  (render [this]
    (dom/h1 "hello world")))

(def ringi (om/factory Ringi))

(def init-state {:handler :none})

(def reconciler
  (om/reconciler
    {:state     init-state
     :normalize true
     :parser    (om/parser {:read p/read :mutate p/mutate})
     :remotes [:remote]
     :send      send-remotes}))

(om/add-root! reconciler MusicEngine (gdom/getElement "root"))

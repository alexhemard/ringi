(ns ringi.core
  (:require [om.next      :as om :refer-macros [defui]]
            [om.dom       :as dom]
            [goog.dom     :as gdom]
            [ringi.util   :refer [send-remotes]]
            [ringi.parser :as p]
            [ringi.routes :refer [routes]]
            [ringi.router :refer [start-router!]]
            [dev]))

(defui Ringi
  Object
  (render [this]
    (dom/h1 nil "hello world")))

(def ringi (om/factory Ringi))

(def init-state {:handler :none})

(def reconciler
  (om/reconciler
    {:state     init-state
     :normalize true
     :parser    (om/parser {:read p/read :mutate p/mutate})
     :remotes [:remote]
     :send      send-remotes}))

(om/add-root! reconciler Ringi (gdom/getElement "root"))

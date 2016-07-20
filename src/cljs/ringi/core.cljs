(ns ringi.core
  (:require [om.next      :as om :refer-macros [defui]]
            [om.dom       :as dom]
            [goog.dom     :as gdom]
            [ringi.util   :refer [send-remotes]]
            [ringi.ui     :as ui]            
            [ringi.parser :as p]
            [ringi.routes :refer [routes]]
            [ringi.router :refer [start-router!]]
            [dev]))

(def handler->component
  {:index       ui/Index
   :login       ui/Login
   :register    ui/Register
   :topics/list ui/Topics
   :topics/show ui/ShowTopic
   :topics/create ui/CreateTopic})

(def handler->factory
  (zipmap (keys handler->component)
    (map om/factory (vals handler->component))))

(def handler->query
  (zipmap (keys handler->component)
    (map om/get-query (vals handler->component))))

(defui Ringi
  static om/IQueryParams
  (params [this]
    {:page-query [] :page-params {}})
  
  static om/IQuery
  (query [this]
    '[:handler ({:current-page ?page-query} ?page-params)])
  
  Object
  (componentWillMount [this]
    (start-router! routes
      {:on-navigate (fn [{:keys [handler params] :as location}]
                      (let [query (handler->query handler [])]
                        (om/set-query! this {:params {:page-query query :page-params params}})
                        (om/transact! this `[(app/navigate ~location)
                                             :current-page])))}))
  
  (render [this]
    (let [{:keys [handler current-page] :as props} (om/props this)]    
    (dom/div nil 
      (let [view (handler->factory handler ui/not-found)]
        (view current-page))))))

(def ringi (om/factory Ringi))

(def init-state {:handler :none})

(def reconciler
  (om/reconciler
    {:state     init-state
     :normalize true
     :parser    (om/parser {:read p/read :mutate p/mutate})
     :remotes   [:remote]
     :send      send-remotes}))

(om/add-root! reconciler Ringi (gdom/getElement "root"))

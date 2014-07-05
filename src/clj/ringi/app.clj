(ns ringi.app
  (:require [com.stuartsierra.component     :as    component]
            [compojure.handler              :as    handler]
            [compojure.route                :as    route]
            [compojure.core                 :refer [GET POST PUT routes context]]
            [ring.util.response             :as    resp]
            [ring.middleware.reload         :refer [wrap-reload]]
            [ring.middleware.session        :refer [wrap-session]]
            [ring.middleware.session.cookie :refer [cookie-store]]
            [cheshire.core                  :as    json]
            [ringi.routes.api               :refer [api-routes]]
            [ringi.routes.auth              :refer [auth-routes]]
            [ringi.auth                     :refer [wrap-user]]
            [ringi.util                     :refer [wrap-slingshot]]))

(defn app-routes [ctx]
  (routes
   (GET "/" [] (resp/redirect "/index.html"))
   (context "/v1" [] (api-routes ctx))
   (auth-routes ctx)
   (route/resources "/")
   (route/not-found "page not foundzzz")))

(defn app
  [ctx]
  (-> (app-routes ctx)
      (wrap-user ctx)
      (wrap-session {:cookie-name "ringi" :store (cookie-store)})
      wrap-slingshot
      handler/api))

(defrecord App [db handler]
  component/Lifecycle
  (start [component]
    (assoc component :handler (app component)))
  (stop [component]
    component))

(defn create-app []
  (map->App {}))

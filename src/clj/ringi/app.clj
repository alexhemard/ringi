(ns ringi.app
  (:require [com.stuartsierra.component     :as    component]
            [compojure.handler              :as    handler]
            [compojure.route                :as    route]
            [compojure.core                 :refer [GET POST PUT PATCH routes context]]
            [ring.util.response             :as    resp]
            [ring.middleware.json           :refer [wrap-json-body wrap-json-response]]
            [ring.middleware.reload         :refer [wrap-reload]]
            [ring.middleware.session        :refer [wrap-session]]
            [ring.middleware.session.cookie :refer [cookie-store]]
            [cheshire.core                  :as     json]
            [hiccup.page                    :refer [html5 include-css include-js]]
            [ringi.routes.api               :refer [api-routes]]
            [ringi.routes.auth              :refer [auth-routes]]
            [ringi.auth                     :refer [wrap-user]]
            [ringi.util                     :refer [wrap-slingshot]]))

(defn index []
  (html5
    [:head {:lang "en"}
     [:title "Ringi"]
     (include-css "/css/ringi.css")
     (include-css "http://fonts.googleapis.com/css?family=Merriweather")]
    [:body
     [:div {:id "root"}
      [:div {:class "menu"}
       [:h1 {:class "menu-title loading"} [:a {:href "/"} "Ringi"]]]]
     (include-js "/js/ringi.js")]))

(defn app-routes [ctx]
  (routes
   (context "/v1" [] (api-routes ctx))
   (auth-routes ctx)
   (route/resources "/")
   (GET "*/*" [] (index)) 
   (route/not-found "404 Not Found")))  

(defn app
  [ctx]
  (-> (app-routes ctx)
      (wrap-user ctx)
      (wrap-session {:cookie-name "ringi" :store (cookie-store)})
      wrap-slingshot
      (wrap-json-body {:keywords? true})
      handler/api
      wrap-json-response))

(defrecord App [datomic handler]
  component/Lifecycle
  (start [component]
    (assoc component :handler (app component)))
  (stop [component]
    component))

(defn create-app []
  (map->App {}))

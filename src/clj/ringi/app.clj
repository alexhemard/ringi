(ns ringi.app
  (:require [com.stuartsierra.component     :as     component]
            [om.next.server                 :as     om]            
            [bidi.ring                      :refer [make-handler resources-maybe]]
            [hiccup.page                    :refer [html5 include-css include-js]]
            [ring.util.response             :refer [response status not-found]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.file           :refer [file-request]]
            [ring.middleware.gzip           :refer [wrap-gzip]]
            [ring.middleware.session        :refer [wrap-session]]
            [ring.middleware.session.cookie :refer [cookie-store]]
            [ringi.parser                   :as     parser]
            [ringi.middleware               :refer [wrap-transit-body
                                                    wrap-transit-params
                                                    wrap-transit-response]]
            [ringi.util                     :refer [wrap-slingshot]]
            [ringi.routes                   :refer [routes]]
            [ringi.assets                   :refer [js-asset css-asset]]))

(def index-page 
  (html5
    [:head {:lang "en"}
     [:title "Ringi"]
     (include-css (css-asset "app.css"))]
    [:body
     [:div {:id "root"}
      [:div {:class "menu"}
       [:h1 {:class "menu-title loading"} [:a {:href "/"} "Ringi"]]]]
     (include-js (js-asset "ringi.js"))]))

(defn index [req]
  (response index-page))

(defn transit-response [data & [status]]
  {:status  (or status 200)
   :headers {"Content-Type" "application/transit+json"}
   :body    data})

(defn api [ctx req]
  (let [datomic (:datomic ctx)]
    (transit-response
      ((om/parser {:read parser/readf :mutate parser/mutatef})
       ctx (:transit-params req)))))

(extend-protocol bidi.ring/Ring
  bidi.ring.ResourcesMaybe
  (request [resource req _]
    (or (file-request req (get-in resource [:options :prefix]))
        (not-found "not found"))))

(defn handler-map [ctx]
  {:index         index
   :login         index
   :register      index
   :topics/list   index
   :topics/create index
   :topics/show   index
   :api           (partial api ctx)   
   :resource      (resources-maybe {:prefix "resources/public"})})

(defn app
  [ctx]
  (-> routes
      (make-handler (handler-map ctx))
      wrap-transit-response
      wrap-transit-params
      wrap-keyword-params
      (wrap-session {:cookie-name "ringi" :store (cookie-store)})
      wrap-slingshot
      wrap-gzip))

(defrecord App [datomic handler]
  component/Lifecycle
  (start [component]
    (assoc component :handler (app component)))
  (stop [component]
    component))

(defn create-app []
  (map->App {}))

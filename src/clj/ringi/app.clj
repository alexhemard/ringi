(ns ringi.app
  (:require [com.stuartsierra.component     :as     component]
            [om.next.server                 :as     om]            
            [bidi.ring                      :refer [make-handler resources-maybe]]
            [hiccup.page                    :refer [html5 include-css include-js]]
            [ring.middleware.gzip           :refer [wrap-gzip]]
            [ring.middleware.session        :refer [wrap-session]]
            [ring.middleware.session.cookie :refer [cookie-store]]
            [ringi.parser                   :as     parser]
            [ringi.middleware               :refer [wrap-transit-body
                                                    wrap-transit-params
                                                    wrap-transit-response]]
            [ringi.util                     :refer [wrap-slingshot]]
            [ringi.assets                   :refer [js-asset css-asset]]))

(defn index [user]
  (html5
    [:head {:lang "en"}
     [:title "Ringi"]
     (when user
       (for [key [:user/name :user/uid]
             :let [value (get user key)]]
         [:meta {(str "user_" (name key)) value}]))
     (include-css (css-asset "app.css"))]
    [:body
     [:div {:id "root"}
      [:div {:class "menu"}
       [:h1 {:class "menu-title loading"} [:a {:href "/"} "Ringi"]]]]
     (include-js (js-asset "ringi.js"))]))

(defn transit-response [data & [status]]
  {:status  (or status 200)
   :headers {"Content-Type" "application/transit+json"}
   :body    data})

(defn api [ctx req]
  (let [datomic (:datomic ctx)]
    (transit-response
      ((om/parser {:read parser/readf :mutate parser/mutatef})
       ctx (:transit-params req)))))

(defn handler-map [ctx]
  {:index         index
   :login         index
   :logout        index
   :register      index
   :topics-index  index
   :topics-show   index
   :topics-create index
   :resource (resources-maybe {:prefix "resources/public"})
   :api (partial api ctx)
   :not-found (fn [req] (not-found "not found"))})

(extend-protocol bidi.ring/Ring
  bidi.ring.ResourcesMaybe
  (request [resource req _]
    (file-request req (get-in resource [:options :prefix]))))

(defn app
  [ctx]
  (-> routes
      (make-handler (handler-map ctx))
      (wrap-session {:cookie-name "ringi" :store (cookie-store)})
      wrap-slingshot
      handler/api
      wrap-json-response
      wrap-gzip))

(defrecord App [datomic handler]
  component/Lifecycle
  (start [component]
    (assoc component :handler (app component)))
  (stop [component]
    component))

(defn create-app []
  (map->App {}))

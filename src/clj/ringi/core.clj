(ns ringi.core
    (:require [compojure.handler :as handler]
              [compojure.route :as route]
              [compojure.core :refer [GET POST defroutes]]
              [ring.util.response :as resp]
              [cheshire.core :as json]
              [clojure.java.io :as io]))

(def votes (atom []))

(defn json-response [data & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "application/json"}
   :body (json/generate-string data)})

(defn init []
  (reset! votes (-> (slurp "votes.json")
                    (json/parse-string true)
                    vec)))
(defroutes app-routes
  (GET "/" [] (resp/redirect "/index.html"))

  (GET "/votes" [] (json-response
                       {:vote @votes}))
  (route/resources "/")
  (route/not-found "Page not found"))

(def app
  (-> #'app-routes
      (handler/api)))

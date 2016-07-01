(ns ringi.router
  (:require [bidi.bidi :as bidi]
            [ringi.routes :refer [routes]]
            [cemerick.url :refer [url map->query]]
            [goog.events :as events]
            [goog.history.EventType :as EventType])
  (:import [goog.history Html5History]
           [goog Uri]))

(defn- retrieve-token [path-prefix location]
  (str (subs (.-pathname location) (count path-prefix))
    (when-let [query (.-search location)]
      query)
    (when-let [hash (second (clojure.string/split (.-href location) #"#"))]
      (str "#" hash))))

(defn- create-url [token path-prefix location]
  (str path-prefix token))

(def token-transformer
  (let [transformer (js/Object.)]
    (set! (.-retrieveToken transformer) retrieve-token)
    (set! (.-createUrl transformer) create-url)
    transformer))

(defn- navigate! [routes path query]
  (let [location (bidi/match-route routes path)
        params (merge (:route-params location) query)
        params (clojure.walk/keywordize-keys params)]
    (-> location
      (dissoc :route-params)
      (assoc  :params params))))

(defn start-router! [routes {:keys [on-navigate history]
                             :or   {history (Html5History. js/window token-transformer)}}]
  (doto history
    (.setUseFragment false)
    (.setPathPrefix  "")
    (.setEnabled     true))

  (events/listen history EventType/NAVIGATE
    (fn [e]
      (let [{:keys [path query] :as u} (url (.-token e))]
        (when-let [location (bidi/match-route routes path)]
          (on-navigate (navigate! routes path query))))))

  (events/removeAll js/document "click") ; hack

  (events/listen js/document "click"
    (fn [e]
      (let [target (.-target e)]
        (when (= (.-tagName target) "A")
          (let [{:keys [query path]} (url (.-href target))]
            (when (bidi/match-route routes path)
              (.preventDefault e)
              (.setToken history (.-href target))))))))

  (let [{:keys [path query]} (url (.-location js/window))]
    (on-navigate (navigate! routes path query))))

(ns ringi.ui
  (:require [om.next :as om :refer-macros [defui]]
            [om.dom  :as dom]))

(defn wrapper [props & children]
  (let [{:keys [id width height color] :or {id "wrapper"}} props]
    (dom/div #js {:className (str id)}
      (dom/header nil
        (dom/nav nil
          (dom/ul nil
            (dom/li nil (dom/a #js {:href "/"} "home"))
            (dom/li nil (dom/a #js {:href "/topics"} "topics"))
            (dom/li nil (dom/a #js {:href "/login"} "login"))
            (dom/li nil (dom/a #js {:href "/register"} "register")))))
      (dom/main nil
        children))))

(defui Vote
  static om/Ident
  (ident [this props]
    [:votes/by-id (:id props)])

  static om/IQuery
  (query [this]
    [:id :author :value])

  Object
  (render [this]
    (dom/div nil "vote")))

(defui Choice
  static om/Ident
  (ident [this props]
    [:choices/by-id (:id props)])

  static om/IQuery
  (query [this]
    [:title {:votes (om/get-query Vote)}])
  Object
  (render [this]
    (dom/div nil "hey")))

(defui Topic
  static om/Ident
  (ident [this props]
    [:topics/by-id (:id props)])

  static om/IQuery
  (query [this]
    [:title :description {:choices (om/get-query Choice)}])

  Object
  (render [this]
    (dom/div nil "hey")))

; pages

(defui Index
  static om/IQuery
  (query [this]
    [:handler])

  Object
  (render [this]
    (dom/h1 nil "Index")))

(defui Login
  static om/IQuery
  (query [this]
    [:handler])

  Object
  (render [this]
    (dom/h1 nil "Login")))

(defui Register
  static om/IQuery
  (query [this]
    [:handler])

  Object
  (render [this]
    (dom/h1 nil "Register")))

(defui ListTopics
  static om/IQuery
  (query [this]
    [:handler {:topics/list (om/get-query Topic)}])

  Object
  (render [this]
    (let [{:keys [topics/list]} (om/props this)]
      (.log js/console list)
      (dom/div nil
        (dom/h1 nil "Topics")
        (dom/div nil list)))))

(defui ShowTopic
  static om/IQuery
  (query [this]
    [:handler])

  Object
  (render [this]
    (dom/h1 nil "Topic")))

(defui CreateTopic
  static om/IQuery
  (query [this]
    [:handler])

  Object
  (render [this]
    (dom/h1 nil "New Topic")))

(defui NotFound
  Object
  (render [this]
    (dom/h1 nil "not implemented :(")))

(def not-found (om/factory NotFound))

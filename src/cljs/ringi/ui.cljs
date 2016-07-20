(ns ringi.ui
  (:require [om.next :as om :refer-macros [defui]]
            [om.dom  :as dom]))

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

(defui Topics
  static om/IQuery
  (query [this]
    [:handler])
  
  Object
  (render [this]
    (dom/h1 nil "Topics")))

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

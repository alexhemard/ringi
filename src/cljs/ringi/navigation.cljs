(ns ringi.navigation
  (:require [om.next :as om]))

(defmulti navigate-to
  (fn [state handler params] handler))

(defmethod navigate-to :default
  [state handler params] state)

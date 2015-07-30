(ns ringi.utils
  (:require [cljs.reader :as reader])
  (:import [goog.ui IdGenerator]))

(defn guid []
  (.getNextUniqueId (.getInstance IdGenerator)))

(defn parse-uuid [maybe-uuid]
  (if (= (type maybe-uuid) cljs.core/UUID)
    maybe-uuid
    (UUID. uuid nil)))

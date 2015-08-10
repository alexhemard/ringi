(ns ringi.dom
  (:require  [clojure.string :as str]))

(defn q [selector]
  (js/document.querySelector selector))

(defn set-value! [el value]
  (set! (.-value el) value))

(defn value [el]
  (let [val (.-value el)]
    (when-not (str/blank? val)
      (str/trim val))))

(defn get-attribute [el attr]
  (value (aget (.-attributes el) attr)))

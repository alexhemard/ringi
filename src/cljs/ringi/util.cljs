(ns ringi.util
  (:require [cognitect.transit :as t]
            [om.next :as om])
  (:import [goog.net XhrIo]))

(defn transit-post [url old-query cb]
  (let [{:keys [query rewrite]} (om/process-roots old-query)]
    (.send XhrIo url
      (fn [e]
        (this-as this
          (let [result (t/read (t/reader :json) (.getResponseText this))]
            (cb (rewrite result) old-query))))
      "POST" (t/write (t/writer :json) query)
      #js {"Content-Type" "application/transit+json"})))

(defmulti send
  (fn [remote _ _] remote))

(defmethod send :default
  [remote query cb])

(defmethod send :remote
  [remote query cb]
  (.log js/console "send" remote query)
  (transit-post "/api" query cb))

(defn send-remotes [remotes cb]
  (println "send" remotes)
  (doseq [[remote query] remotes]
    (send remote query cb)))

(ns ringi.util
    (:require [clojure.java.jdbc :as jdbc]
              [crypto.password.pbkdf2 :as p]
              [cheshire.core      :as json]
              [ringi.uuid :refer [b64->uuid uuid->b64]]
              [slingshot.slingshot :refer [try+ throw+]])
    (:import (java.util UUID)))

(defn reorder-map [map keys]
  (apply array-map (mapcat identity (for [k keys :when (k map)] [k (k map)]))))

(defn parse-int
  [s]
  (try
    (cond
     (string? s) (Integer/parseInt (re-find #"\A-?\d+" s))
     (number? s) s
     :else nil)
    (catch Exception e)))

(defn uuid? [u]
  (instance? UUID u))

(defn parse-uuid
  [uuid]
  (try
    (cond
     (string? uuid) (try
                      (UUID/fromString uuid)
                      (catch Exception e (b64->uuid uuid)))
     (uuid? uuid) uuid
     :else nil)
    (catch Exception e nil)))

(defn parse-date [date]
  (if (instance? java.sql.Date date)
    date
    (try
      (new java.sql.Date
           (.getTime
            (.parse (java.text.SimpleDateFormat. "MM-dd-yyyy") date)))
      (catch Exception _ nil))))

(defn current-timestamp
  []
  (java.sql.Timestamp.
   (System/currentTimeMillis)))

(defn jazz-up-json [data]
  (clojure.walk/prewalk
   (fn [x]
     (if (uuid? x) (uuid->b64 x) x)) data))

(defn json-response [data & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "application/json"}
   :body (json/generate-string (-> data
                                   (jazz-up-json))
                               {:pretty true})})

(defn unauthorized-response []
  {:status 401})

(defn unauthorized! []
  (throw+ {:type ::unauthorized}))

(defn wrap-slingshot
  [f]
  (fn [req]
    (try+
      (f req)
      (catch [:type ::unauthorized] _
        (unauthorized-response)))))

(ns ringi.util
  (:require  [datomic.api :as d]
             [crypto.password.pbkdf2 :as p]
             [cheshire.core          :as json]
             [ringi.uuid             :refer [b64->uuid
                                             uuid->b64]]
             [clojure.string :refer [blank?]]
             [clojure.walk           :refer [keywordize-keys prewalk]]
             [slingshot.slingshot    :refer [try+ throw+]])
    (:import (java.util UUID)))

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
  (prewalk
   (fn [x]
     (if (uuid? x) (uuid->b64 x) x)) data))

(defn json-response [data & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "application/json"}
   :body (json/generate-string data {:pretty true})})

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

(defn not-blank? [val]
  (if val
    (not (blank? val))
    true))

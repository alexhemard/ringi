(ns ringi.auth
  (:require [environ.core           :refer [env]]
            [oauth.client           :as    oauth]))

(def consumer (oauth/make-consumer (env :twitter-token  "***REMOVED***")
                                   (env :twitter-secret "***REMOVED***")
                                   "https://api.twitter.com/oauth/request_token"
                                   "https://api.twitter.com/oauth/access_token"
                                   "https://api.twitter.com/oauth/authorize"
                                   :hmac-sha1))



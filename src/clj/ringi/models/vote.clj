(ns ringi.models.vote
    (:require [datomic.api  :as d]
              [ringi.mapper :refer [defmap]]))

(def vote-values-m
  {"yes" :vote.value/yes
   "no"  :vote.value/no
   "ok"  :vote.value/ok})

(defn vote [conn user choice value]
  (if-let [val (get vote-values-m value)]
    @(d/transact conn [[:vote user choice value]])))

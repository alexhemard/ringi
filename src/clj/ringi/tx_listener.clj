(ns ringi.tx-listener
  (:require [datomic.api :as d]
            [clojure.tools.logging :as log]
            [clojure.core.async :refer [chan pub close! >! <!! >!! sliding-buffer put!] :as async]
            [com.stuartsierra.component :as component]))

(def topic-changed-q
  '[:find ?t ?e 
    :in $ [[?e _ _]]
    :where [?t :topic/uid]
    (or-join [?e]
      [?e :topic/uid]
      [?t :choices ?e]
      (and [?t :choices ?c]
        [?c :votes ?e]))])

(defrecord TxListener [datomic tx-report-queue tx-pub tx-ch tx-thread]
  component/Lifecycle
  (start [component]
    (let [tx-ch           (chan (sliding-buffer 1024))
          tx-pub          (pub tx-ch :topic)
          tx-report-queue (d/tx-report-queue (:conn datomic))
          tx-thread
          (async/thread
            (loop [tx (.take tx-report-queue)]
              (when-not (= :shutdown tx)
                (let [{:keys [db-after tx-data]} tx
                      results (d/q topic-changed-q db-after tx-data)]
                  (doall (for [[topic entity] results]
                           (put! tx-ch {:topic topic
                                        :entity entity})))
                  (recur (.take tx-report-queue))))))]
      (assoc component
        :tx-report-queue tx-report-queue
        :tx-thread       tx-thread
        :tx-pub          tx-pub
        :tx-ch           tx-ch)))
  (stop [component]
    (.add tx-report-queue :shutdown)
    (close! tx-ch)
    (<!! tx-thread)
    (d/remove-tx-report-queue (:conn datomic))
    (assoc component
      :tx-report-queue nil
      :tx-thread       nil
      :tx-pub          nil
      :tx-ch           nil)))

(defn create-tx-listener []
  (map->TxListener {}))

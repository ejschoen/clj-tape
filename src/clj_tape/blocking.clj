(ns clj-tape.blocking
  (:require [clojure.core.async :as async])
  (:require [clj-tape.core :as ctc]))

(defprotocol ConcurrentQueue
  "Protocol for concurrently-accessed queues, supporting atomic take!"
  (is-closed? [_] "Return true if queue is closed.")
  (take! [_] [_ timeout timeout-val] "Atomically peek-and-remove"))

(defn make-blocking-queue
  [queue & {:keys [timeout] :as options}]
  {:pre [(satisfies? ctc/Queue queue)
         (or (nil? timeout) (integer? timeout))]}
  (let [notify-put-channel (async/chan (async/dropping-buffer 1)) 
        notify-take-channel(async/chan (async/dropping-buffer 1))
        closed (ref false)
        wait (fn [do-timeout & [wait-timeout]]
               (loop [[val port] (async/alts!!
                                  (if (or wait-timeout timeout)
                                    [notify-put-channel (async/timeout (or wait-timeout timeout))]
                                    [notify-put-channel]))]
                 (if (not= port notify-put-channel)
                   (if do-timeout
                     (if @closed
                       :closed
                       :timeout)
                     (if @closed
                       :closed
                       (recur (async/alts!!
                               (if (or wait-timeout timeout)
                                 [notify-put-channel (async/timeout (or wait-timeout timeout))]
                                 [notify-put-channel])))))
                   (if val
                     :ready
                     :closed))))]
    (reify
      ConcurrentQueue
      (is-closed? [_] @closed)
      (take! [queue]
        (take! queue nil nil))
      (take! [_  timeout timeout-val]
        (if @closed
          nil
          (let [[status val]
                (locking queue
                  (if (ctc/is-empty? queue)
                    [:empty nil]
                    [:ready (let [val (ctc/peek queue)]
                              (async/>!! notify-take-channel true)
                              (ctc/remove! queue)
                              val)]))]
            (if (= :empty status)
              (loop [status (wait true timeout)]
                (case status
                  :timeout timeout-val
                  :closed nil
                  :ready (let [[status val] (locking queue
                                              (let [val (ctc/peek queue)]
                                                (if val
                                                  (do (async/>!! notify-take-channel true)
                                                      (ctc/remove! queue)
                                                      [:ready val])
                                                  [:timeout nil])))]
                           (if (= status :ready)
                             val
                             (recur (wait true timeout))))))
              val))))
      ctc/Queue
      (ctc/is-empty? [_] (ctc/is-empty? queue))
      (ctc/put! [_ data]
        (if @closed
          nil
          (locking queue
            (ctc/put! queue data)
            (async/>!! notify-put-channel true)
            true)))
      (ctc/peek [_]
        (if (ctc/is-empty? queue)
          (case (wait true)
            :timeout :timeout
            :ready (do (async/<!! notify-put-channel)
                       (ctc/peek queue))
            :closed nil)
          (ctc/peek queue)))
      (ctc/peek [this n]
        (for [i (range n)]
          (do (when (ctc/is-empty? queue)
                (wait false))
              (ctc/peek this))))
      (ctc/as-list [this]
        (ctc/peek this (ctc/size queue)))
      (ctc/remove! [_]
        (if (ctc/is-empty? queue)
          (case (wait true)
            :timeout :timeout
            :ready (do (ctc/remove! queue)
                       (async/<!! notify-put-channel)
                       true)
            :closed nil)
          (ctc/remove! queue)))
      (ctc/remove! [this n]
        (doseq [i (range n)]
          (do
            (when (ctc/is-empty? queue)
              (wait false))
            (ctc/remove! this))))
      (ctc/clear! [_] (ctc/clear! queue))
      (ctc/size [_] (ctc/size queue))
      (ctc/close! [_]
        (dosync (ref-set closed true))
        (while (not (ctc/is-empty? queue))
          (ctc/remove! queue))
        (ctc/close! queue))
      (ctc/delete! [this]
        (ctc/close! this)
        (ctc/delete! queue))
      )))

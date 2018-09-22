(ns clj-tape.blocking
  (:require [clj-tape.core :as ctc]))

(defprotocol ConcurrentQueue
  "Protocol for concurrently-accessed queues, supporting atomic take!"
  (is-closed? [_] "Return true if queue is closed.")
  (take! [_] [_ timeout timeout-val] "Atomically peek-and-remove"))

(defn make-blocking-queue
  [queue & {:keys [timeout] :as options}]
  {:pre [(satisfies? ctc/Queue queue)
         (or (nil? timeout) (integer? timeout))]}
  (let [closed (ref false)
        wait (fn [do-timeout & [wait-timeout]]
               (if (= 0 (or wait-timeout timeout))
                 :timeout
                 (loop []
                   (.wait queue (or wait-timeout timeout))
                   (cond @closed :closed
                         (ctc/is-empty? queue) (if do-timeout :timeout
                                                   (recur)
                                                   )
                         :else :ready))))]
    (reify
      ConcurrentQueue
      (is-closed? [_] @closed)
      (take! [queue]
        (take! queue nil nil))
      (take! [_  timeout timeout-val]
        (if @closed
          nil
          (locking queue
            (let [[status val]
                  (if (ctc/is-empty? queue)
                    [:empty nil]
                    [:ready (let [val (ctc/peek queue)]
                              (ctc/remove! queue)
                              val)])]
              (if (= :empty status)
                (loop [status (wait true timeout)]
                  (case status
                    :timeout timeout-val
                    :closed nil
                    :ready (let [[status val] (let [val (ctc/peek queue)]
                                                (if val
                                                  (do 
                                                    (ctc/remove! queue)
                                                    [:ready val])
                                                  [:timeout nil]))]
                             (if (= status :ready)
                               val
                               (recur (wait true timeout))))))
                val)))))
      ctc/Queue
      (ctc/is-empty? [_] (ctc/is-empty? queue))
      (ctc/put! [_ data]
        (if @closed
          nil
          (locking queue
            (ctc/put! queue data)
            (.notifyAll queue)
            true)))
      (ctc/peek [_]
        (if @closed
          nil
          (locking queue
            (if (ctc/is-empty? queue)
              (case (wait true)
                :timeout :timeout
                :ready (ctc/peek queue)
                :closed nil)
              (ctc/peek queue)))))
      (ctc/peek [this n]
        (locking queue
          (for [i (range n)]
            (do (when (ctc/is-empty? queue)
                  (wait false))
                (ctc/peek this)))))
      (ctc/as-list [this]
        (ctc/peek this (ctc/size queue)))
      (ctc/remove! [_]
        (if @closed
          nil
          (locking queue
            (if (ctc/is-empty? queue)
              (case (wait true)
                :timeout :timeout
                :ready (do (ctc/remove! queue)
                           true)
                :closed nil)
              (ctc/remove! queue)))))
      (ctc/remove! [this n]
        (if @closed
          nil
          (locking queue
            (doseq [i (range n)]
              (do
                (when (ctc/is-empty? queue)
                  (wait false))
                (ctc/remove! this))))))
      (ctc/clear! [_] (locking queue (ctc/clear! queue)))
      (ctc/size [_] (locking queue (ctc/size queue)))
      (ctc/close! [_]
        (locking queue
          (dosync (ref-set closed true))
          (while (not (ctc/is-empty? queue))
            (ctc/remove! queue))
          (ctc/close! queue)))
      (ctc/delete! [this]
        (locking queue
          (ctc/close! this)
          (ctc/delete! queue)))
      )))

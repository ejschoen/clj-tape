(ns clj-tape.blocking-test
  (:require [clojure.java.io :as io])
  (:require [clojure.test :refer :all]
            [clj-tape.core :as clj-tape]
            [clj-tape.blocking :as clj-blocking]))

(deftest in-memory-test
  (let [queue (clj-blocking/make-blocking-queue
               (clj-tape/make-object-queue)
               :timeout 500)]
    (let [f1 (future (Thread/sleep 1000) (clj-tape/put! queue "Hello"))]
      (is (= (clj-tape/peek queue) :timeout)))
    (Thread/sleep 500)
    (is (= (clj-tape/peek queue) "Hello"))
    (is (not (clj-tape/is-empty? queue)))
    (clj-tape/remove! queue)
    (is (clj-tape/is-empty? queue))
    (let [f1 (future (Thread/sleep 100) (clj-tape/put! queue "Goodbye"))]
      (is (= (clj-tape/peek queue) "Goodbye")))
    (is (not (clj-tape/is-empty? queue)))
    (clj-tape/remove! queue)
    (is (clj-tape/is-empty? queue))
    (let [f1 (future (Thread/sleep 10) (clj-tape/close! queue))]
      (is (nil? (clj-tape/peek queue) )))))

(deftest timeout-test
  (let [queue (clj-blocking/make-blocking-queue
               (clj-tape/make-object-queue))]
    (future (clj-tape/put! queue "Hello"))
    (is (= "Hello" (clj-blocking/take! queue 500 :timeout)))
    (is (= :timeout (clj-blocking/take! queue 500 :timeout)))))


(deftest many-entries-test
  (let [filename "queue-test"
        n 1000]
    (try 
      (do 
        (when (.exists (io/as-file filename))
          (.delete (io/as-file filename)))
        (let [queue (clj-blocking/make-blocking-queue
                     (clj-tape/make-queue (io/as-file filename))
                     :timeout 1000)]
          (future (doseq [i (range n)]
                    (Thread/sleep (rand 10))
                    (clj-tape/put! queue (format "Message %d" i))))
          (doseq [i (range n)]
            (do (is (= (format "Message %d" i)
                       (clj-tape/peek queue)))
                (clj-tape/remove! queue)))
          (is (clj-tape/is-empty? queue))
          (clj-tape/close! queue))
        (.delete (io/as-file filename))
        (let [queue (clj-blocking/make-blocking-queue
                     (clj-tape/make-queue (io/as-file filename))
                     :timeout 10)]
          (future (doseq [i (range n)]
                    (Thread/sleep (rand 100))
                    (clj-tape/put! queue (format "Message %d" i))))
          (let [all (clj-tape/as-list queue)]
            (map-indexed (fn [i m]
                           (is (= (format "Message %d" i) m)))
                         all)
            (clj-tape/remove! queue (count all))
            (is (clj-tape/is-empty? queue))
            (clj-tape/close! queue))))
      (finally (when (.exists (io/as-file filename))
                 (.delete (io/as-file filename)))))))

(deftest multi-readers-test
    (let [n-items 100
          n-threads 4]
      (is (= 0 (mod n-items n-threads)))
      (let [queue (clj-blocking/make-blocking-queue
                   (clj-tape/make-object-queue)
                   :timeout 1000)]
        (future (do (doseq [i (range n-items)]
                      (do (Thread/sleep (rand 100))
                          (clj-tape/put! queue (format "Message %04d" i))))
                    (while (> (clj-tape/size queue) 0)
                      (Thread/sleep 10))
                    (clj-tape/close! queue)
                    (println "Thread" (.getName (Thread/currentThread)) ": Queue is closed")))
        (let [futures (for [i (range n-threads)]
                        (future (doall
                                 (for [i-item (range)
                                       :let [message (clj-blocking/take! queue)]
                                       :while message]
                                   (do #_(println (.getName (Thread/currentThread)) message)
                                       message)))))
              messages (sort (apply concat (map deref futures)))]
          (println "All futures are realized")
          (is (= n-items (count messages)))
          (map-indexed (fn [i m]
                         #_(println m)
                         (is (= m (format "Message %04d" i))))
                       messages)
          (is (clj-tape/is-empty? queue))))))

(deftest delete-test
  (let [path "test/resources/to-be-deleted"]
    (when (.exists (io/as-file path))
      (.delete (io/as-file path))
      (is (not (.exists (io/as-file path)))))
    (let [queue (clj-blocking/make-blocking-queue (clj-tape/make-queue path))]
      (println "Created queue.  File exists:" (.exists (io/as-file path)))
      (is (.exists (io/as-file path)))
      (.delete! queue)
      (is (not (.exists (io/as-file path)))))))

(deftest test-empty-queue-zero-timeout-take
  (let [queue (clj-blocking/make-blocking-queue (clj-tape/make-object-queue))]
    (is (= :empty (clj-blocking/take! queue 0 :empty)))))

(deftest test-block-forever
  (let [queue (clj-blocking/make-blocking-queue (clj-tape/make-object-queue))
        fut (future (clj-blocking/take! queue))]
    (Thread/sleep 3000)
    (is (not (realized? fut)))
    (clj-tape/put! queue "Hello")
    (Thread/sleep 500)
    (is (realized? fut))
    (is (= "Hello" @fut))
  ))

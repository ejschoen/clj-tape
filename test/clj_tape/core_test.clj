(ns clj-tape.core-test
  (:import [java.io File])
  (:require [clojure.java.io :as io])
  (:require [clojure.test :refer :all]
            [clj-tape.core :refer :all]))

(deftest in-memory-test
  (let [queue (make-object-queue)]
    (is (is-empty? queue))
    (put! queue "Hello")
    (is (not (is-empty? queue)))
    (is (= "Hello" (peek queue)))
    (remove! queue)
    (is (is-empty? queue))))

(deftest in-file-test
  (let [file (io/as-file "queue-test")]
    (.deleteOnExit file)
    (let [queue (make-object-queue file)]
      (is (is-empty? queue))
      (put! queue "Hello")
      (is (not (is-empty? queue)))
      (is (= "Hello" (peek queue)))
      (remove! queue)
      (is (is-empty? queue)))))

(deftest many-entries-test
  (let [file (io/as-file "queue-test")
        n 1000]
    (.deleteOnExit file)
    (let [queue (make-object-queue file)
          start (System/nanoTime)]
      (doseq [i (range n)]
        (put! queue "Hello"))
      (println "puts done")
      (let [end-put (System/nanoTime)]
        (doseq [i (range n)]
          (do (is (= "Hello" (peek queue)))
              (remove! queue)))
        (is (is-empty? queue))
        (let [end (System/nanoTime)]
          (println n "puts took" (float (/ (- end-put start) 1000000000)) "seconds.")
          (println n "gets took" (float (/ (- end end-put) 1000000000)) "seconds."))))) )
        

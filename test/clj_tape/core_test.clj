(ns clj-tape.core-test
  (:require [clojure.java.io :as io])
  (:require [clojure.test :refer :all]
            [clj-tape.core :as clj-tape]))

(deftest in-memory-test
  (let [queue (clj-tape/make-object-queue)]
    (is (clj-tape/is-empty? queue))
    (clj-tape/put! queue "Hello")
    (is (not (clj-tape/is-empty? queue)))
    (is (= "Hello" (clj-tape/peek queue)))
    (clj-tape/remove! queue)
    (is (clj-tape/is-empty? queue))))

(deftest in-file-test
  (let [file (io/as-file "queue-test")]
    (.deleteOnExit file)
    (when (.exists file)
      (.delete file))
    (let [queue (clj-tape/make-object-queue file)]
      (is (clj-tape/is-empty? queue))
      (clj-tape/put! queue "Hello")
      (is (not (clj-tape/is-empty? queue)))
      (is (= "Hello" (clj-tape/peek queue)))
      (clj-tape/remove! queue)
      (is (clj-tape/is-empty? queue)))))

(deftest in-queue-file-test
  (let [file (io/as-file "queue-test")]
    (.deleteOnExit file)
    (when (.exists file)
      (.delete file))
    (let [queue (clj-tape/make-queue file)]
      (is (clj-tape/is-empty? queue))
      (clj-tape/put! queue "Hello")
      (is (not (clj-tape/is-empty? queue)))
      (is (= "Hello" (clj-tape/peek queue)))
      (clj-tape/remove! queue)
      (is (clj-tape/is-empty? queue)))))

(deftest many-entries-test
  (let [file (io/as-file "queue-test")
        n 1000]
    (.deleteOnExit file)
    (when (.exists file)
      (.delete file))
    (let [queue (clj-tape/make-object-queue file)
          start (System/nanoTime)]
      (doseq [i (range n)]
        (clj-tape/put! queue "Hello"))
      (println "puts done")
      (is (= n (count (clj-tape/peek queue n))))
      (let [end-put (System/nanoTime)]
        (doseq [i (range n)]
          (do (is (= "Hello" (clj-tape/peek queue)))
              (clj-tape/remove! queue)))
        (is (clj-tape/is-empty? queue))
        (let [end (System/nanoTime)]
          (println n "puts took" (float (/ (- end-put start) 1000000000)) "seconds.")
          (println n "gets took" (float (/ (- end end-put) 1000000000)) "seconds."))))) )
        
(deftest persistence-test
  (let [queue (clj-tape/make-queue "test/resources/test-queue")]
    (is (not (clj-tape/is-empty? queue)))
    (is (= 1 (clj-tape/size queue)))
    (is (= "Hello" (clj-tape/peek queue)))
    (clj-tape/close! queue)))

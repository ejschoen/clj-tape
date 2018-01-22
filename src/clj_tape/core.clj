(ns clj-tape.core
  (:require [clojure.java.io :as io])
  (:require [cheshire.core :as cheshire])
  (:import [java.io ByteArrayInputStream OutputStream])
  (:import [com.squareup.tape2 ObjectQueue ObjectQueue$Converter QueueFile QueueFile$Builder]))



(defn make-queue-file
  "Make a Tape2 QueueFile.  file must be coerceable to a java.io.File, via clojure.java.io/as-file."
  [file]
  (.build (QueueFile$Builder. (io/as-file file))))

(defn make-object-queue
  "Make an object queue.  With no args, make an in-memory queue.  With a queue-file arg (coercible to java.io.File via clojure.java.io/as-file),
   makes a persistent queue."
  ([]
   (ObjectQueue/createInMemory))
  ([queue-file]
   (ObjectQueue/create (make-queue-file queue-file)
                       (reify ObjectQueue$Converter
                         (from [this bytes]
                           (cheshire/parse-smile bytes))
                         (^void toStream [this obj ^OutputStream output-stream]
                          (let [bytes (cheshire/generate-smile obj)]
                            (.write output-stream bytes)))))))


(defn is-empty?
  "Return true if the queue is empty."
  [^ObjectQueue queue]
  (.isEmpty queue))

(defn put!
  "Put an object into the queue."
  [^ObjectQueue queue, obj]
  (.add queue obj))

(defn peek
  "Peek at the first entry in the queue, or get a list of entries at the head of the queue, upto max elements."
  ([^ObjectQueue queue]
   (.peek queue))
  ([^ObjectQueue queue max]
   (.peek queue ^Integer (int max))))

(defn as-list!
  "Get the entire content of the queue as a list."
  [^ObjectQueue queue]
  (.asList queue))

(defn remove!
  "Remove the first entry, or max entries from the queue."
  ([^ObjectQueue queue]
   (^void .remove queue))
  ([^ObjectQueue queue max]
   (^void .remove queue max)))

(defn clear!
  "Clear the queue."
  [^ObjectQueue queue]
  (^void .clear queue))

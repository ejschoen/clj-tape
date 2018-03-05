(ns clj-tape.core
  (:require [clojure.java.io :as io])
  (:require [cheshire.core :as cheshire])
  (:import [java.io ByteArrayInputStream OutputStream])
  (:import [com.squareup.tape2 ObjectQueue ObjectQueue$Converter QueueFile QueueFile$Builder]))

(defprotocol Queue
  "Protocol for queues, either object or simple binary data"
  (is-empty? [_] "Return true if the queue is empty.")
  (put! [_ data] "Put an object into the queue.")
  (peek [_] [_ n] "Peek at the first entry in the queue, or upto n entries in the queue.")
  (as-list [_] "Peek at all entries in the queue, as a sequence.")
  (remove! [_] [_ n] "Remove the first entry from the queue, or upto n entries in the queue.")
  (clear! [_] "Clear the queue of all entries.")
  (size [_] "Return count of items in queue")
  (close! [_] "Close the queue.")
  (delete! [_] "Close the queue and delete its persistent backing file."))

(defprotocol Converter
  (from [_ bytes] "Convert from byte array")
  (to [_ obj] "Convert to byte array"))

(defn make-queue-file
  "Make a Tape2 QueueFile.  file must be coerceable to a java.io.File, via clojure.java.io/as-file."
  [file]
  (.build (QueueFile$Builder. (io/as-file file))))


(defn- reify-object-queue
  [^ObjectQueue queue]
  (reify Queue
    (is-empty? [_] (.isEmpty queue))
    (put! [_ data] (.add queue data))
    (peek [_] (.peek queue))
    (peek [_ n] (.peek queue (int n)))
    (as-list [_] (.asList queue))
    (remove! [_] (.remove queue))
    (remove! [_ n] (.remove queue (int n)))
    (clear! [_] (.clear queue))
    (size [_] (.size queue))
    (close! [_] (.close queue))
    (delete! [_]
      (.close queue)
      (let [queue-file (.file queue)]
        (when queue-file
          (.delete (.file queue-file)))))))

(defn- reify-queue
  [^QueueFile queue converter]
  {:pre [(satisfies? Converter converter)]}
  (reify Queue
    (is-empty? [_] (.isEmpty queue))
    (put! [_ data] (.add queue (.to converter data)))
    (peek [_] (.from converter (.peek queue)))
    (peek [_ n] (map #(.from converter ^bytes %) queue))
    (as-list [this] (.peek this (.size queue)))
    (remove! [_] (.remove queue))
    (remove! [_ n] (.remove queue (int n)))
    (clear! [_] (.clear queue))
    (size [_] (.size queue))
    (close! [_] (.close queue))
    (delete! [_] (.close queue) (.delete (.file queue)))))

(defn make-queue
  ([queue-file]
   (make-queue queue-file
               (reify Converter
                 (from [_ bytes] (cheshire/parse-smile bytes))
                 (to [_ obj] (cheshire/generate-smile obj)))))
  ([queue-file converter]
   (reify-queue
    (make-queue-file queue-file) converter)))

(defn make-object-queue
  "Make an object queue.  With no args, make an in-memory queue.  With a queue-file arg (coercible to java.io.File via clojure.java.io/as-file),
   makes a persistent queue."
  ([]
   (reify-object-queue (ObjectQueue/createInMemory)))
  ([queue-file]
   (reify-object-queue
    (ObjectQueue/create (make-queue-file queue-file)
                        (reify ObjectQueue$Converter
                          (from [this bytes]
                            (cheshire/parse-smile bytes))
                          (^void toStream [this obj ^OutputStream output-stream]
                           (let [bytes (cheshire/generate-smile obj)]
                             (.write output-stream bytes))))))))

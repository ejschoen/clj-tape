# clj-tape

A Clojure library that wraps Square's (persistent) queue-related [Tape](https://github.com/square/tape) classes.  For serialization, the library uses [Cheshire](https://github.com/dakrone/cheshire)'s SMILE serialization.

## Usage

### Leiningen dependencies

![Travis-CI build status](https://travis-ci.org/ejschoen/clj-tape.svg?branch=master)

```clojure
[org.clojars.ejschoen/clj-tape "0.4.3"]
```

```clojure
(ns your.namespace
  (:require [clj-tape.core :as clj-tape]))
```

### Make a queue

```clojure
(def file-queue (clj-tape/make-queue "my-file-queue"))
(def in-memory-queue (clj-tape/make-object-queue))
(def persistent-queue (clj-tape/make-object-queue "my-queue-file"))
```

Tape provides file queues, which persist byte arrays, and object queues, which wrap file queues
with a converter that serializes and deserializes objects to and from byte arrays.  For Clojure,
file queues and object queues offer the same capabilities.  In clj-tape, serialization and
deserialization use JSON [SMILE](https://en.wikipedia.org/wiki/Smile_(data_interchange_format)),
as implemented by Cheshire.

clj-tape defines a `Converter` protocol:

```clojure
(defprotocol Converter
  (from [_ bytes] "Convert from byte array")
  (to [_ obj] "Convert to byte array"))
```

Custom serialization and deserialization is possible with file queues:

```clojure
(defn make-a-queue
  [my-converter]
  {:pre [(satisfies? clj-tape/Converter my-converter)]}
  (clj-tape/make-queue "my-queue" my-converter))
```

### Queue operations

```clojure
(clj-tape/put! persistent-queue "Hello")
(= "Hello" (clj-tape/peek persistent-queue))
(= 1 (clj-tape/size persistent-queue))
(for [item (clj-tape/peek persistent-queue 10)] ...)
(clj-tape/remove! persistent-queue)
(clj-tape/remove! persistent-queue 10)
(clj-tape/is-empty? persistent-queue)
(clj-tape/clear! persistent-queue)
(clj-tape/close! persistent-queue)
(clj-tape/delete! pesistent-queue)
```

### Blocking operations

Tape's queue's do not support blocking on peek or remove.  For multi-threaded scenarios akin to
Clojure's core.async channels, clj-tape supports a blocking wrapper that allows threads to wait
for items to appear on the queue, while other threads generate items to place on the queue.

```clojure
(ns your.namespace
  (:require [clj-tape core blocking]))
(def blocking-queue (clj-tape.blocking/make-blocking-queue
                      (clj-tape.core/make-queue "my-blocking-queue")
                      :timeout 100))
(future (Thread/sleep 1000) (clj-tape.core/put! blocking-queue "Hello"))
(is (= :timeout (clj-tape.core/take! blocking-queue 100 :timeout)))
(Thread/sleep 1000)
(is (= "Hello" (clj-tape.core/peek blocking-queue)))
(clj-tape.core/remove! blocking-queue)
(future (Thread/sleep 10) (clj-tape.core/close! blocking-queue))
(is (thrown? Exception (clj-tape.core/peek blocking-queue)))
```

### Multi-threaded readers and writers

Blocking queues can be used with multiple readers and writers.  Use `take!` instead of `peek` and `remove!` to
atomically take from a queue.  `take!` can optionally time out, returning a designated timeout value, and returns `nil`
when the queue is closed.  Note that `take!` is only available with clj-tape's blocking queues.

## License

Copyright © 2018 Eric Schoen

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

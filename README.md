# clj-tape

A Clojure library that wraps Square's queue-related [Tape](https://github.com/square/tape) classes.  For serialization, the library uses [Cheshire](https://github.com/dakrone/cheshire)'s SMILE serialization.

## Usage

Leiningen dependencies

![Travis-CI build status](https://travis-ci.org/ejschoen/clj-tape.svg?branch=master)

```clojure
[org.clojars.ejschoen/clj-tape "0.1.0"]
```



```clojure
(ns your.namespace
  (:require [clj-tape.core :as clj-tape]))
```

Make a queue

```clojure
(def file-queue (clj-tape/make-queue "my-file-queue"))
(def in-memory-queue (clj-tape/make-object-queue))
(def persistent-queue (clj-tape/make-object-queue "my-queue-file"))
```

Queue operations

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
```

## License

Copyright © 2018 Eric Schoen

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

# clj-tape

A Clojure library that wraps Square's queue-related Tape classes.

## Usage

Leiningen dependencies



```clojure
(ns your.namespace
  (:require [clj-tape.core :as clj-tape]))
```

Make a queue

```clojure
(def in-memory-queue (clj-tape/make-object-queue))
(def persistent-queue (clj-tape/make-object-queue "my-queue-file"))
```

Queue operations

```clojure
(put! persistent-queue "Hello")
(peek persistent-queue)
(peek persistent-queue 10)
(remove! persistent-queue)
(remove! persistent-queue 10)
(is-empty? persistent-queue)
(clear! persistent-queue)
```

## License

Copyright © 2018 i2kConnect LLC

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

## onyx-manifold

Onyx plugin providing read and write facilities for Manifold. Based on / shamelessly ripped off of : [onyx-core-async](https://github.com/onyx-platform/onyx/blob/master/src/onyx/plugin/core_async.clj)

#### Installation

In your project file:

```clojure
[onyx-manifold "0.2.0"]
```

In your peer boot-up namespace:

```clojure
(:require [onyx.plugin.manifold])
```

#### Catalog entries

##### read-from-stream

```clojure
{:onyx/name :in
 :onyx/ident :manifold/read-from-stream
 :onyx/type :input
 :onyx/medium :manifold
 :onyx/batch-size batch-size
 :onyx/max-peers 1
 :onyx/doc "Reads segments from a manifold stream"}
```

##### write-to-stream

```clojure
{:onyx/name :out
 :onyx/ident :manifold/write-to-stream
 :onyx/type :output
 :onyx/medium :manifold
 :onyx/batch-size batch-size
 :onyx/max-peers 1
 :onyx/doc "Writes segments to a manifold stream"}
```

#### Attributes

This plugin does not use any attributes.

#### Lifecycle Arguments

References to manifold streams must be injected for both the input and output tasks.

##### `read-from-stream`

```clojure
(def in-stream (stream (inc n-messages)))

(defn inject-in-stream [event lifecycle]
  {:manifold/stream in-stream})

(def in-calls
  {:lifecycle/before-task-start inject-in-stream})
```

##### `write-to-stream`

```clojure
(def out-stream (stream (inc n-messages)))

(defn inject-out-stream [event lifecycle]
  {:manifold/stream out-stream})

(def out-calls
  {:lifecycle/before-task-start inject-out-stream})
```

##### `lifecycles`

```clojure
(def lifecycles
  [{:lifecycle/task :in
    :lifecycle/calls :onyx.plugin.manifold-test/in-calls}
   {:lifecycle/task :in
    :Lifecycle/calls :onyx.plugin.manifold/reader-calls}
   {:lifecycle/task :out
    :lifecycle/calls :onyx.plugin.manifold-test/out-calls}
   {:lifecycle/task :out
```

#### Functions

##### `take-segments!`

This additional function is provided as a utility for removing segments
from a stream until `:done` is found. After `:done` is encountered, all prior segments,
including `:done`, are returned in a seq.

#### Contributing

Pull requests into the master branch are welcomed.

#### License

Copyright © 2014 Michael Drogalis

Copyright © 2015 mccraigmccraig of the clan mccraig

Distributed under the Eclipse Public License, the same as Clojure.

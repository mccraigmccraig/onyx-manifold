(ns onyx.plugin.manifold-test
  (:require [manifold.stream :refer [take! try-take! put! stream]]
            [midje.sweet :refer :all]
            [onyx.plugin.manifold :refer [take-segments!]]
            [onyx.test-helper :refer [load-config]]
            [onyx.api]))

(def id (java.util.UUID/randomUUID))

(def config (load-config))

(def env-config (assoc (:env-config config) :onyx/id id))

(def peer-config (assoc (:peer-config config) :onyx/id id))

(def env (onyx.api/start-env env-config))

(def peer-group (onyx.api/start-peer-group peer-config))

(def n-messages 15000)

(def batch-size 40)

(defn my-inc [{:keys [n] :as segment}]
  (assoc segment :n (inc n)))

(def catalog
  [{:onyx/name :in
    :onyx/ident :manifold/read-from-stream
    :onyx/type :input
    :onyx/medium :manifold
    :onyx/batch-size batch-size
    :onyx/max-peers 1
    :onyx/doc "Reads segments from a manifold stream"}

   {:onyx/name :inc
    :onyx/fn :onyx.plugin.manifold-test/my-inc
    :onyx/type :function
    :onyx/max-peers 1
    :onyx/batch-size batch-size}

   {:onyx/name :out
    :onyx/ident :manifold/write-to-stream
    :onyx/type :output
    :onyx/medium :manifold
    :onyx/batch-size batch-size
    :onyx/max-peers 1
    :onyx/doc "Writes segments to a manifold stream"}])

(def workflow [[:in :inc] [:inc :out]])

(def in-stream (stream (inc n-messages)))

(def out-stream (stream (inc n-messages)))

(defn inject-in-stream [event lifecycle]
  {:manifold/stream in-stream})

(defn inject-out-stream [event lifecycle]
  {:manifold/stream out-stream})

(def in-calls
  {:lifecycle/before-task-start inject-in-stream})

(def out-calls
  {:lifecycle/before-task-start inject-out-stream})

(def lifecycles
  [{:lifecycle/task :in
    :lifecycle/calls :onyx.plugin.manifold-test/in-calls}
   {:lifecycle/task :in
    :lifecycle/calls :onyx.plugin.manifold/reader-calls}
   {:lifecycle/task :out
    :lifecycle/calls :onyx.plugin.manifold-test/out-calls}
   {:lifecycle/task :out
    :lifecycle/calls :onyx.plugin.manifold/writer-calls}])

(doseq [n (range n-messages)]
  (put! in-stream {:n n}))

(put! in-stream :done)

(def v-peers (onyx.api/start-peers 8 peer-group))

(onyx.api/submit-job
 peer-config
 {:catalog catalog :workflow workflow
  :lifecycles lifecycles
  :task-scheduler :onyx.task-scheduler/balanced})

(def results (take-segments! out-stream))

(doseq [v-peer v-peers]
  (onyx.api/shutdown-peer v-peer))

(let [expected (set (map (fn [x] {:n (inc x)}) (range n-messages)))]
  (fact (set (butlast results)) => expected)
  (fact (last results) => :done))

(onyx.api/shutdown-peer-group peer-group)
(onyx.api/shutdown-env env)

(ns onyx.plugin.manifold-test
  (:require [manifold.stream :refer [take! try-take! put! close! stream]]
            [onyx.peer.task-lifecycle-extensions :as l-ext]
            [onyx.plugin.manifold]
            [midje.sweet :refer :all]
            [onyx.api]))

(def id (java.util.UUID/randomUUID))

(def scheduler :onyx.job-scheduler/round-robin)

(def env-config
  {:hornetq/mode :vm
   :hornetq/server? true
   :hornetq.server/type :vm
   :zookeeper/address "127.0.0.1:2185"
   :zookeeper/server? true
   :zookeeper.server/port 2185
   :onyx/id id
   :onyx.peer/job-scheduler scheduler})

(def peer-config
  {:hornetq/mode :vm
   :zookeeper/address "127.0.0.1:2185"
   :onyx/id id
   :onyx.peer/inbox-capacity 100
   :onyx.peer/outbox-capacity 100
   :onyx.peer/job-scheduler scheduler})

(def env (onyx.api/start-env env-config))

(def batch-size 25)

(def workflow
  [[:in :increment]
   [:increment :out]])

(def catalog
  [{:onyx/name :in
    :onyx/ident :manifold/read-from-stream
    :onyx/type :input
    :onyx/medium :manifold
    :onyx/consumption :concurrent
    :onyx/batch-size batch-size
    :onyx/batch-timeout 200
    :onyx/max-peers 1
    :onyx/doc "Reads segments from a manifold stream"}

   {:onyx/name :increment
    :onyx/fn :onyx.plugin.manifold-test/increment
    :onyx/type :function
    :onyx/consumption :concurrent
    :onyx/batch-size batch-size
    :onyx/batch-timeout 200}

   {:onyx/name :out
    :onyx/ident :manifold/write-to-stream
    :onyx/type :output
    :onyx/medium :manifold
    :onyx/consumption :concurrent
    :onyx/batch-size batch-size
    :onyx/batch-timeout 200
    :onyx/max-peers 1
    :onyx/doc "Writes segments to a manifold stream"}])

(defn increment [segment]
  (assoc segment :n (inc (:n segment))))

(def in-stream (stream 10000))

(def out-stream (stream 10000))

(defmethod l-ext/inject-lifecycle-resources :in
  [_ _] {:manifold/in-stream in-stream})

(defmethod l-ext/inject-lifecycle-resources :out
  [_ _] {:manifold/out-stream out-stream})

(def n-segments 100)

(doseq [n (range n-segments)]
  (put! in-stream {:n n}))

(put! in-stream :done)

(close! in-stream)

(def v-peers (onyx.api/start-peers! 1 peer-config))

(onyx.api/submit-job peer-config
                     {:catalog catalog
                      :workflow workflow
                      :task-scheduler :onyx.task-scheduler/round-robin})

(def results (doall (map (fn [_] @(take! out-stream)) (range (inc n-segments)))))

(let [expected (set (map (fn [x] {:n (inc x)}) (range n-segments)))]
  (fact (set (butlast results)) => expected)
  (fact (last results) => :done))

(doseq [v-peer v-peers]
  (onyx.api/shutdown-peer v-peer))

(onyx.api/shutdown-env env)

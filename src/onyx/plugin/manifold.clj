(ns onyx.plugin.manifold
  (:require [manifold.stream :refer [take! try-take! put!]]
            [onyx.peer.pipeline-extensions :as p-ext]))

(defmethod p-ext/read-batch [:input :manifold]
  [{:keys [onyx.core/task-map manifold/in-stream]}]
  (let [batch-size (:onyx/batch-size task-map)
        ms (or (:onyx/batch-timeout task-map) 1000)
        batch (->> (range batch-size)
                   (map (fn [_] {:input :manifold
                                 :message @(try-take! in-stream :onyx.plugin.manifold/fail ms :onyx.plugin.manifold/timeout)}))
                   (filter (comp not #{:onyx.plugin.manifold/fail :onyx.plugin.manifold/timeout} :message)))]
    {:onyx.core/batch (doall batch)}))

(defmethod p-ext/decompress-batch [:input :manifold]
  [{:keys [onyx.core/batch]}]
  {:onyx.core/decompressed (filter identity (map :message batch))})

(defmethod p-ext/strip-sentinel [:input :manifold]
  [{:keys [onyx.core/decompressed]}]
  {:onyx.core/tail-batch? (= (last decompressed) :done)
   :onyx.core/requeue? false
   :onyx.core/decompressed (remove (partial = :done) decompressed)})

(defmethod p-ext/apply-fn [:input :manifold]
  [{:keys [onyx.core/decompressed]}]
  {:onyx.core/results decompressed})

(defmethod p-ext/apply-fn [:output :manifold]
  [{:keys [onyx.core/decompressed]}]
  {:onyx.core/results decompressed})

(defmethod p-ext/compress-batch [:output :manifold]
  [{:keys [onyx.core/results]}]
  {:onyx.core/compressed results})

(defmethod p-ext/write-batch [:output :manifold]
  [{:keys [onyx.core/compressed manifold/out-stream]}]
  (doseq [segment compressed]
    (put! out-stream segment))
  {})

(defmethod p-ext/seal-resource [:output :manifold]
  [{:keys [manifold/out-stream]}]
  (put! out-stream :done)
  {})

(defn take-segments!
  "Takes segments off the stream until :done is found.
   Returns a seq of segments, including :done."
  [stream]
  (loop [x []]
    (let [segment @(take! stream)]
      (let [stack (conj x segment)]
        (if-not (= segment :done)
          (recur stack)
          stack)))))

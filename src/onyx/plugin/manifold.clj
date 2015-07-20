(ns onyx.plugin.manifold
  (:require [clojure.core.async :refer [chan >!! <!! alts!! timeout]]
            [manifold.stream :refer [connect take! put!]]
            [onyx.peer.pipeline-extensions :as p-ext]
            [onyx.static.default-vals :refer [defaults]]
            [taoensso.timbre :refer [debug info] :as timbre]))

(defn inject-reader
  [event lifecycle]
  (assert (:manifold/stream event) ":manifold/stream not found - add it using a :before-task-start lifecycle")
  (let [stream-chan (chan)]
    (connect (:manifold/stream event) stream-chan)
    {:manifold/stream-chan stream-chan
     :manifold/pending-messages (atom {})
     :manifold/drained? (atom false)
     :manifold/retry-chan (chan 10000)
     :manifold/retry-count (atom 0)}))

(defn log-retry-count
  [event lifecycle]
  (info "manifold input plugin stopping. Retry count:" @(:manifold/retry-count event))
  {})

(defn inject-writer
  [event lifecycle]
  (assert (:manifold/stream event) ":manifold/stream not found - add it using a :before-task-start lifecycle")
  {})

(def reader-calls
  {:lifecycle/before-task-start inject-reader
   :lifecycle/after-task-stop log-retry-count})

(def writer-calls
  {:lifecycle/before-task-start inject-writer})

(defmethod p-ext/read-batch :manifold/read-from-stream
  [{:keys [onyx.core/task-map manifold/stream-chan manifold/retry-chan
           manifold/pending-messages manifold/drained?] :as event}]
  (let [pending (count @pending-messages)
        max-pending (or (:onyx/max-pending task-map) (:onyx/max-pending defaults))
        batch-size (:onyx/batch-size task-map)
        max-segments (min (- max-pending pending) batch-size)
        ms (or (:onyx/batch-timeout task-map) (:onyx/batch-timeout defaults))
        timeout-ch (timeout ms)
        batch (if (pos? max-segments)
                (loop [segments [] cnt 0]
                  (if (= cnt max-segments)
                    segments
                    (if-let [message (first (alts!! [retry-chan stream-chan timeout-ch] :priority true))]
                      (do
                        (recur (conj segments
                                     {:id (java.util.UUID/randomUUID)
                                      :input :manifold
                                      :message message})
                               (inc cnt)))
                      segments)))
                (<!! timeout-ch))]
    (doseq [m batch]
      (swap! pending-messages assoc (:id m) (:message m)))
    (when (and (= 1 (count @pending-messages))
               (= (count batch) 1)
               (= (:message (first batch)) :done))
      (reset! drained? true))
    {:onyx.core/batch batch}))

(defmethod p-ext/ack-message :manifold/read-from-stream
  [{:keys [manifold/pending-messages]} message-id]
  (swap! pending-messages dissoc message-id))

(defmethod p-ext/retry-message :manifold/read-from-stream
  [{:keys [manifold/pending-messages manifold/retry-count manifold/retry-chan]} message-id]
  (when-let [msg (get @pending-messages message-id)]
    (swap! pending-messages dissoc message-id)
    (when-not (= msg :done)
      (swap! retry-count inc))
    (>!! retry-chan msg)))

(defmethod p-ext/pending? :manifold/read-from-stream
  [{:keys [manifold/pending-messages]} message-id]
  (get @pending-messages message-id))

(defmethod p-ext/drained? :manifold/read-from-stream
  [{:keys [manifold/drained? manifold/pending-messages] :as event}]
  @drained?)

(defmethod p-ext/write-batch :manifold/write-to-stream
  [{:keys [onyx.core/results manifold/stream] :as event}]
  (doseq [msg (mapcat :leaves results)]
    (put! stream (:message msg)))
  {})

(defmethod p-ext/seal-resource :manifold/write-to-stream
  [{:keys [manifold/stream]}]
  (put! stream :done))

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

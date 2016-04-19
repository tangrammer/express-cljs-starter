(ns node-cljs.api.protocols
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :as async :refer [put! chan <! >! close!]])
  )

(defprotocol Response
  (send [_ res]))


(extend-protocol Response
  cljs.core.async.impl.channels/ManyToManyChannel
  (send [this res]
    (go (let [r (<! this)]
          (.json res (clj->js r)))))
  cljs.core/PersistentArrayMap
  (send [this res]
    (.json res (clj->js this)))
  nil
  (send [this res]
    (.json res nil))
  )

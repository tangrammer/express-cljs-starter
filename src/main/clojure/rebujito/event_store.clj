(ns rebujito.event-store
  (:require
    [com.stuartsierra.component :as component]
    [clj-time.core :as t]
    [schema.core :as s]
    [rebujito.protocols :as protocols]
    [rebujito.mongo :as mongo]))

(def allowed-event-types #{:manual-payment-failed
                           :micros-topup-failed})

(defrecord EventStore [config]
  component/Lifecycle
  (start [this]
    (mongo/start* this))
  (stop [this] this)

  protocols/EventStore
  (log [this event-type data]
    (let [mongo-record {:type event-type
                        :created-at (t/now)
                        :data data}]
      (s/validate (apply s/enum allowed-event-types) (keyword event-type))
      (mongo/insert!* this mongo-record)))
 )

(defn new-event-store []
  (map->EventStore {:collection :events}))

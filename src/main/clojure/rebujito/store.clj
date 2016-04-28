(ns rebujito.store
  (:require
   [rebujito.protocols :as protocols]
   [com.stuartsierra.component :refer [system-map system-using using] :as component]
   [plumbing.core :refer [defnk]]
   [rebujito.store.mocks :as mocks]

   ))


(defrecord ProdStore []
  component/Lifecycle
  (start [this]
    this)
  (stop [this] this)
  protocols/Store
  (get-card [this]
    (assoc mocks/card :id :prod))
  (get-payment-method [this]
    (assoc mocks/payment-method :id :prod))
  )

(defrecord MockStore []
  component/Lifecycle
  (start [this]
    this)
  (stop [this] this)
  protocols/Store
  (get-card [this]
    (assoc mocks/card :id :dev))
  (get-payment-method [this]
    (assoc mocks/payment-method :id :dev))

)


(defn new-prod-store []
  (map->ProdStore {}))

(defn new-mock-store []
  (map->MockStore {}))

(ns rebujito.store
  (:require
   [rebujito.protocols :as protocols]
   [com.stuartsierra.component  :as component]
   [rebujito.store.mocks :as mocks]

   ))


(defrecord ProdStore []
  component/Lifecycle
  (start [this]
    this)
  (stop [this] this)
  protocols/Store
  (get-card [this]
    (assoc mocks/card :target-environment :prod))
  (get-payment-method-detail [this]
    (assoc mocks/get-payment-method-detail :target-environment :prod))
  (post-payment-method [this]
    (assoc mocks/post-payment-method :target-environment :prod))
  (get-payment-method [this]
    (mapv #(assoc % :target-environment :prod) mocks/get-payment-method ))
  )

(defrecord MockStore []
  component/Lifecycle
  (start [this]
    this)
  (stop [this] this)
  protocols/Store
  (get-card [this]
    (assoc mocks/card :target-environment :dev))
  (get-payment-method-detail [this]
    (assoc mocks/get-payment-method-detail :target-environment :dev))
  (post-payment-method [this]
    (assoc mocks/post-payment-method :target-environment :dev))
  (get-payment-method [this]
    (mapv #(assoc % :target-environment :dev) mocks/get-payment-method ))
)


(defn new-prod-store []
  (map->ProdStore {}))

(defn new-mock-store []
  (map->MockStore {}))

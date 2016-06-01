(ns rebujito.store
  (:require
   [taoensso.timbre :as log]
   [rebujito.protocols :as protocols]
   [com.stuartsierra.component  :as component]
   [rebujito.store.mocks :as mocks]))




(defrecord ProdStore []
  component/Lifecycle
  (start [this]
    this)
  (stop [this] this)
  protocols/Store
  (get-cards [this]
    (assoc mocks/card :target-environment :prod))
  (get-card [this data]
    (assoc mocks/card :target-environment :prod))
  (get-payment-method-detail [this data]
    (assoc mocks/get-payment-method-detail :target-environment :prod))
  (put-payment-method-detail [this data]
    (assoc mocks/put-payment-method-detail :target-environment :prod))
  (post-payment-method [this data]
    (assoc mocks/post-payment-method :target-environment :prod))
  (get-payment-method [this]
    (mapv #(assoc % :target-environment :prod) mocks/get-payment-method))
  (post-token-resource-owner [this]
    (assoc mocks/post-token-resource-owner :target-environment :prod))
  (post-refresh-token [this]
    (assoc mocks/post-refresh-token :target-environment :prod))
  (get-profile [this]
      (assoc mocks/me-profile :target-environment :prod))
  )


(defrecord MockStore []
  component/Lifecycle
  (start [this]
    this)
  (stop [this] this)
  protocols/Store
  (get-cards [this]
    (assoc mocks/card :target-environment :dev))
  (get-card [this data]
    (assoc mocks/card :target-environment :dev))
  (get-payment-method-detail [this data]
    (assoc mocks/get-payment-method-detail :target-environment :dev))
  (put-payment-method-detail [this data]
    (assoc mocks/put-payment-method-detail :target-environment :dev))
  (post-payment-method [this data]
    (assoc mocks/post-payment-method :target-environment :dev))
  (get-payment-method [this]
    (mapv #(assoc % :target-environment :dev) mocks/get-payment-method))
  (post-token-resource-owner [this]
    (assoc mocks/post-token-resource-owner  :target-environment :dev))
  (post-refresh-token [this]
    (assoc mocks/post-refresh-token :target-environment :dev))
  (get-profile [this]
      (assoc mocks/me-profile :target-environment :dev))

  )




(defn new-prod-store []
  (map->ProdStore {}))

(defn new-mock-store []
  (map->MockStore {}))

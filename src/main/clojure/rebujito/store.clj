(ns rebujito.store
  (:require
    [rebujito.protocols :as protocols]
    [com.stuartsierra.component :as component]
    [rebujito.store.mocks :as mocks]
    [rebujito.payment-gateway :as payment-gateway]
    [rebujito.api.util :refer :all]))

(defrecord ProdStore [config]
  component/Lifecycle
  (start [this]
    (let [gateway (payment-gateway/new-prod-payment-gateway (:payment-gateway config))]
      (assoc this :payment-gateway gateway)))
  (stop [this]
        (assoc this :payment-gateway nil))
  protocols/Store
  (get-card [this]
    (assoc mocks/card :target-environment :prod))
  (get-payment-method-detail [this]
    (assoc mocks/get-payment-method-detail :target-environment :prod))
  (post-payment-method [this data]
    (let [{:keys [status vaultId]} (protocols/create-card-token (:payment-gateway this) data)]
      {:status status
       :result vaultId}))
  (get-payment-method [this]
    (mapv #(assoc % :target-environment :prod) mocks/get-payment-method))
  (post-token-resource-owner [this]
    (assoc mocks/post-token-resource-owner :target-environment :prod))
  (post-refresh-token [this]
    (assoc mocks/post-refresh-token :target-environment :prod)))


(defrecord MockStore [config]
  component/Lifecycle
  (start [this]
    this)
  (stop [this] this)
  protocols/Store
  (get-card [this]
    (assoc mocks/card :target-environment :dev))
  (get-payment-method-detail [this]
    (assoc mocks/get-payment-method-detail :target-environment :dev))
  (post-payment-method [this data]
    ["201" (assoc mocks/post-payment-method :target-environment :dev)])
  (get-payment-method [this]
    (mapv #(assoc % :target-environment :dev) mocks/get-payment-method))
  (post-token-resource-owner [this]
    (assoc mocks/post-token-resource-owner  :target-environment :dev))
  (post-refresh-token [this]
    (assoc mocks/post-refresh-token :target-environment :dev)))




(defn new-prod-store [config]
  (map->ProdStore config))

(defn new-mock-store [config]
  (map->MockStore config))

;; deprecated !!
(ns rebujito.store
  (:require
   [manifold.deferred :as d]
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
  #_(get-cards [this]
    (assoc mocks/card :target-environment :prod))
  #_(get-deferred-card [this data]
    (let [d* (d/deferred)]
      (if-let [card-data (assoc mocks/card :target-environment :prod)]
        (d/success! d* card-data)
        (d/error! d* (ex-info (str "STORE ERROR!")
                              {:type :store
                               :status 404
                               :body ["Card Not Found"]})))
      d*))
  #_(get-deferred-payment-method-detail [this data]
    (let [d* (d/deferred)]
    (if-let [payment-method-data (assoc mocks/get-payment-method-detail :target-environment :prod)]
      (d/success! d* payment-method-data)
      (d/error! d* (ex-info (str "API ERROR!")
                            {:type :store
                             :status 404
                             :body ["Payment Method Not Found"]})))
    d*))
  #_(put-payment-method-detail [this data]
    (assoc mocks/put-payment-method-detail :target-environment :prod))
  #_(post-payment-method [this data]
    (let [d* (d/deferred)]
      (if-let [payment-method-data  (assoc mocks/post-payment-method :target-environment :prod)]
        (d/success! d* payment-method-data)
        (d/error! d* (ex-info (str "API ERROR!")
                              {:type :store
                               :status 500
                               :body ["post-payment-method Error "]})))
      d*))
  #_(get-payment-method [this]
    (mapv #(assoc % :target-environment :prod) mocks/get-payment-method))
  #_(post-token-resource-owner [this]
    (assoc mocks/post-token-resource-owner :target-environment :prod))
  #_(post-refresh-token [this]
    (assoc mocks/post-refresh-token :target-environment :prod))
  )

(defrecord MockStore []
  component/Lifecycle
  (start [this]
    this)
  (stop [this] this)
  protocols/Store
  #_(get-cards [this]
    (assoc mocks/card :target-environment :dev))
  #_(get-deferred-card [this data]
    (let [d* (d/deferred)]
      (if-let [card-data (assoc mocks/card :target-environment :dev)]
        (d/success! d* card-data)
        (d/error! d* (ex-info (str "STORE ERROR!")
                              {:type :store
                               :status 404
                               :body ["Card Not Found"]})))
      d*))
  #_(get-deferred-payment-method-detail [this data]
    (let [d* (d/deferred)]
    (if-let [payment-method-data (assoc mocks/get-payment-method-detail :target-environment :dev)]
      (d/success! d* payment-method-data)
      (d/error! d* (ex-info (str "API ERROR!")
                            {:type :store
                             :status 404
                             :body ["Payment Method Not Found"]})))
    d*))
  #_(put-payment-method-detail [this data]
    (assoc mocks/put-payment-method-detail :target-environment :dev))
  #_(post-payment-method [this data]
    (let [d* (d/deferred)]
      (if-let [payment-method-data  (assoc mocks/post-payment-method :target-environment :dev)]
        (d/success! d* payment-method-data)
        (d/error! d* (ex-info (str "API ERROR!")
                              {:type :store
                               :status 500
                               :body ["post-payment-method Error "]})))
      d*))
  #_(get-payment-method [this]
    (mapv #(assoc % :target-environment :dev) mocks/get-payment-method))
  #_(post-token-resource-owner [this]
    (assoc mocks/post-token-resource-owner  :target-environment :dev))
  #_(post-refresh-token [this]
    (assoc mocks/post-refresh-token :target-environment :dev))
  )

(defn new-prod-store []
  (map->ProdStore {}))

(defn new-mock-store []
  (map->MockStore {}))

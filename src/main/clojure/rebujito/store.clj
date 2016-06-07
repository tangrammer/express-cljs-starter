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
  (get-cards [this]
    (assoc mocks/card :target-environment :prod))
  (get-deferred-card [this data]
    (let [d* (d/deferred)]
      (if-let [card-data (assoc mocks/card :target-environment :prod)]
        (d/success! d* card-data)
        (d/error! d* (ex-info (str "STORE ERROR!")
                              {:type :store
                               :status 404
                               :body ["Card Not Found"]})))
      d*))
  (get-deferred-payment-method-detail [this data]
    (let [d* (d/deferred)]
    (if-let [payment-method-data (assoc mocks/get-payment-method-detail :target-environment :prod)]
      (d/success! d* payment-method-data)
      (d/error! d* (ex-info (str "API ERROR!")
                            {:type :store
                             :status 404
                             :body ["Payment Method Not Found"]})))
    d*))
  (put-payment-method-detail [this data]
    (assoc mocks/put-payment-method-detail :target-environment :prod))
  (post-payment-method [this data]
    (let [d* (d/deferred)]
      (if-let [payment-method-data  (assoc mocks/post-payment-method :target-environment :prod)]
        (d/success! d* payment-method-data)
        (d/error! d* (ex-info (str "API ERROR!")
                              {:type :store
                               :status 500
                               :body ["post-payment-method Error "]})))
      d*))
  (get-payment-method [this]
    (mapv #(assoc % :target-environment :prod) mocks/get-payment-method))
  (post-token-resource-owner [this]
    (assoc mocks/post-token-resource-owner :target-environment :prod))
  (post-refresh-token [this]
    (assoc mocks/post-refresh-token :target-environment :prod))
  (get-deferred-profile [this]
    (let [d* (d/deferred)]
      (if-let [profile-data (assoc mocks/me-profile :target-environment :prod)]
        (d/success! d* profile-data)
        (d/error! d* (ex-info (str "STORE ERROR!")
                              {:type :store
                               :status 404
                               :body  ["Profile Not Found"]})))
      d*)))

(defrecord MockStore []
  component/Lifecycle
  (start [this]
    this)
  (stop [this] this)
  protocols/Store
  (get-cards [this]
    (assoc mocks/card :target-environment :dev))
  (get-deferred-card [this data]
    (let [d* (d/deferred)]
      (if-let [card-data (assoc mocks/card :target-environment :dev)]
        (d/success! d* card-data)
        (d/error! d* (ex-info (str "STORE ERROR!")
                              {:type :store
                               :status 404
                               :body ["Card Not Found"]})))
      d*))
  (get-deferred-payment-method-detail [this data]
    (let [d* (d/deferred)]
    (if-let [payment-method-data (assoc mocks/get-payment-method-detail :target-environment :dev)]
      (d/success! d* payment-method-data)
      (d/error! d* (ex-info (str "API ERROR!")
                            {:type :store
                             :status 404
                             :body ["Payment Method Not Found"]})))
    d*))
  (put-payment-method-detail [this data]
    (assoc mocks/put-payment-method-detail :target-environment :dev))
  (post-payment-method [this data]
    (let [d* (d/deferred)]
      (if-let [payment-method-data  (assoc mocks/post-payment-method :target-environment :dev)]
        (d/success! d* payment-method-data)
        (d/error! d* (ex-info (str "API ERROR!")
                              {:type :store
                               :status 500
                               :body ["post-payment-method Error "]})))
      d*))
  (get-payment-method [this]
    (mapv #(assoc % :target-environment :dev) mocks/get-payment-method))
  (post-token-resource-owner [this]
    (assoc mocks/post-token-resource-owner  :target-environment :dev))
  (post-refresh-token [this]
    (assoc mocks/post-refresh-token :target-environment :dev))
  (get-deferred-profile [this]
    (let [d* (d/deferred)]
      (if-let [profile-data (assoc mocks/me-profile :target-environment :dev)]
        (d/success! d* profile-data)
        (d/error! d* (ex-info (str "STORE ERROR!")
                              {:type :store
                               :status 404
                               :body  ["Profile Not Found"]})))
      d*)))

(defn new-prod-store []
  (map->ProdStore {}))

(defn new-mock-store []
  (map->MockStore {}))

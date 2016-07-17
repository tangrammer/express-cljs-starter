(ns rebujito.resource-pool
  (:require
    [manifold.deferred :as d]
    [rebujito.protocols :as protocols]
    [clj-http.client :as http-c]
    [cheshire.core :as json]
    [com.stuartsierra.component  :as component]
    [schema.core :as s]
    [taoensso.timbre :as log]))

(defrecord ProdCardResourcePool [base-url token]

  component/Lifecycle
  (start [this] this)
  (stop [this] this)

  protocols/CardResourcePool
  (checkout-physical-card [this card-number pin]
    (d/future
      (Thread/sleep 100)
      #_(throw (ex-info "pin doesn't match" {:status 409 :body {:code "400" :message "pin doesn't match"}}))
      {:ok :ok}
      ))
    )

(defn new-prod-card-resource-pool [config]
  (map->ProdCardResourcePool config))

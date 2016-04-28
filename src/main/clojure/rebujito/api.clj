(ns rebujito.api
  (:require
    [com.stuartsierra.component :as component]
    [plumbing.core :refer [fnk defnk]]
    [schema.core :as s]
    [rebujito.api.resources :as resources]))

(defn api [store]
  ["/me" [[[ "/" :id "/fake"] (-> (resources/fake store)
                                  (assoc :id ::fake))]
          [["/paymentmethods/" :payment-mehod-id]
           (-> (resources/get-payment-method store)
               (assoc :id ::index))]
          ["/cards/register-digital"
           [["" (-> (resources/register-digital-card store)
                    (assoc :id ::index))]]]]])

(s/defrecord ApiComponent [store]
  component/Lifecycle
  (start [component]
    (assoc component :routes (api store)))
  (stop [component]
        component))

(defn new-api-component []
  (->
   (map->ApiComponent {})))

(ns rebujito.api
  (:require
    [com.stuartsierra.component :as component]

    [schema.core :as s]
    [rebujito.api.resources :as resources]))

(defn api [store]
  ["/me" [[[ "/" :id "/fake"]  (resources/fake store)]
          ["/paymentmethods" [["" (resources/post-payment-method store)]
                              [["/" :payment-mehod-id] (resources/get-payment-method store)]]

           ]
          ["/cards/register-digital"
           [["" (resources/register-digital-card store)]]]]])

(s/defrecord ApiComponent [store]
  component/Lifecycle
  (start [component]
    (assoc component :routes (api store)))
  (stop [component]
        component))

(defn new-api-component []
  (->
   (map->ApiComponent {})))

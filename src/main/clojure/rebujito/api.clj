(ns rebujito.api
  (:require
    [com.stuartsierra.component :as component]

    [schema.core :as s]
    [rebujito.api.resources :as resources]
    [rebujito.api.resources.payment :as payment]
    [rebujito.api.resources.oauth :as oauth]
    ))

(defn api [store]
  ["/" [["oauth/token" (oauth/token-resource-owner store)]
        ["me" [["/paymentmethods" [["" (payment/methods store)]
                                   [["/" :payment-mehod-id] (payment/method-detail store)]]]
               ["/cards/register-digital"
                [["" (resources/register-digital-card store)]]]]]]])

(s/defrecord ApiComponent [store]
  component/Lifecycle
  (start [component]
    (assoc component :routes (api store)))
  (stop [component]
        component))

(defn new-api-component []
  (->
   (map->ApiComponent {})))

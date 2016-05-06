(ns rebujito.api
  (:require
    [com.stuartsierra.component :as component]

    [schema.core :as s]
    [rebujito.api.resources :as resources]
    [rebujito.api.resources.payment :as payment]
    [rebujito.api.resources.oauth :as oauth]
    [rebujito.api.resources.account :as account]
    [rebujito.api.resources.card :as card]
    ))

(defn api [store]
  ["/" [["account/create" (account/create store)]
        ["oauth/token" (oauth/token-resource-owner store)]
        ["me" [["/paymentmethods" [["" (payment/methods store)]
                                   [["/" :payment-mehod-id] (payment/method-detail store)]]]
               ["/cards"
                [["" (card/get-cards store)]]]
               ["/cards/register-digital"
                [["" (card/register-digital-cards store)]]]]]]])

(s/defrecord ApiComponent [store]
  component/Lifecycle
  (start [component]
    (assoc component :routes (api store)))
  (stop [component]
        component))

(defn new-api-component []
  (->
   (map->ApiComponent {})))

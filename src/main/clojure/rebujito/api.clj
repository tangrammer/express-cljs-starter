(ns rebujito.api
  (:require
    [com.stuartsierra.component :as component]

    [schema.core :as s]
    [rebujito.api.resources :as resources]
    [rebujito.api.resources.payment :as payment]
    [rebujito.api.resources.oauth :as oauth]
    [rebujito.api.resources.account :as account]
    [rebujito.api.resources.card :as card]
    [rebujito.api.resources.social-profile :as social-profile]))


(defn api [store mimi]
  ["/" [["account/create" (-> (account/create store mimi)
                              (assoc :id ::account/create))]
        ["oauth/token" (-> (oauth/token-resource-owner store)
                           (assoc :id ::oauth/token-resource-owner))]
        ["me" [["/cards"
                [["" (-> (card/get-cards store)
                         (assoc :id ::card/get-cards))]

                 ["/register"
                  (-> (card/register-physical store)
                      (assoc :id ::card/register-physical))]

                 ["/register-digital" (-> (card/register-digital-cards store)
                                          (assoc :id ::card/register-digital-cards))]

                 ["/" [[["" :card-id] (-> (card/unregister store)(assoc :id ::card/unregister))]]]]]



               ["/paymentmethods" [["" (-> (payment/methods store)
                                           (assoc :id ::payment/methods))]
                                   [["/" :payment-mehod-id] (-> (payment/method-detail store)
                                                                (assoc :id ::payment/method-detail))]]]
               ["/socialprofile/account" (-> (social-profile/account store)
                                             (assoc :id ::social-profile/account))]]]]])


(s/defrecord ApiComponent [store mimi]
  component/Lifecycle
  (start [component]
    (assoc component :routes (api store mimi)))
  (stop [component]
        component))

(defn new-api-component []
  (->
   (map->ApiComponent {})))

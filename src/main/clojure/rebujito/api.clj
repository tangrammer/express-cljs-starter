(ns rebujito.api
  (:require
    [com.stuartsierra.component :as component]

    [schema.core :as s]
    [rebujito.api.resources.payment :as payment]
    [rebujito.api.resources.profile :as profile]
    [rebujito.api.resources.oauth :as oauth]
    [rebujito.api.resources.account :as account]
    [rebujito.api.resources.card :as card]

    [rebujito.api.resources.social-profile :as social-profile]))

(defn api [store mimi user-store authorizer crypto authenticator payment-gateway api-client-store]
  ["/" [["account/create" (-> (account/create store mimi user-store crypto)
                              (assoc :id ::account/create))]
        ["oauth/token" (-> (oauth/token-resource-owner store user-store authorizer crypto api-client-store)
                           (assoc :id ::oauth/token-resource-owner))]
        ["me" [["" (-> (account/get-user store mimi user-store authorizer authenticator)
                              (assoc :id ::account/get-user))]
               ["/profile"  (-> (profile/me store mimi user-store authorizer authenticator)
                                  (assoc :id ::profile/me))]
               ["/cards"
                [["" (-> (card/get-cards store)
                         (assoc :id ::card/get-cards))]

                 ["/register"
                  (-> (card/register-physical store mimi user-store)
                      (assoc :id ::card/register-physical))]

                 ["/register-digital" (-> (card/register-digital-cards store)
                                          (assoc :id ::card/register-digital-cards))]

                 ["/" [[["" :card-id] (-> (card/unregister store)(assoc :id ::card/unregister))]]]]]



               ["/paymentmethods" [["" (-> (payment/methods store payment-gateway)
                                           (assoc :id ::payment/methods))]
                                   [["/" :payment-method-id] (-> (payment/method-detail store)
                                                                (assoc :id ::payment/method-detail))]]]
               ["/socialprofile/account" (-> (social-profile/account store)
                                             (assoc :id ::social-profile/account))]]]]]
  )

(s/defrecord ApiComponent [store mimi user-store authorizer crypto authenticator payment-gateway api-client-store]
  component/Lifecycle
  (start [component]
    (assoc component :routes (api store mimi  user-store authorizer crypto authenticator payment-gateway api-client-store)))
  (stop [component]
        component))

(defn new-api-component []
  (map->ApiComponent {}))

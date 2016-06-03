(ns rebujito.api
  (:require
    [com.stuartsierra.component :as component]
    [schema.core :as s]
    [yada.resource :refer (resource)]
    [rebujito.api
     [util :as util]]
    [rebujito.api.resources
     [payment :as payment]
     [account :as account]
     [card :as card]
     [login :as login]
     [oauth :as oauth]
     [profile :as profile]
     [social-profile :as social-profile]
     ]))

(defn api [store mimi user-store authorizer crypto authenticator payment-gateway api-client-store mailer]
  ["/" [["health" (resource (-> {:methods
                                 {:get {:consumes [{:media-type #{"application/json"}
                                                    :charset "UTF-8"}]
                                        :response (read-string (slurp (clojure.java.io/resource "VERSION.edn"))) }}}
                                (merge (util/common-resource :meta))
                                (merge {:access-control {}})) )]
        ["account/create" (-> (account/create store mimi user-store crypto authenticator authorizer)
                              (assoc :id ::account/create))]
        ["oauth/token" (-> (oauth/token-resource-owner store user-store authorizer crypto api-client-store)
                           (assoc :id ::oauth/token-resource-owner))]
        ["login/forgot-password" (-> (login/forgot-password authorizer mailer authorizer authenticator)
                                               (assoc :id ::login/forgot-password))]
        ["me" [
               ["" (-> (account/me store mimi user-store authorizer authenticator)
                       (assoc :id ::account/get-user))]
               ["/login/validate-password" (-> (login/validate-password user-store crypto authorizer authenticator)
                                               (assoc :id ::login/validate-password))]
               ["/profile"  (-> (profile/me store mimi user-store authorizer authenticator)
                                  (assoc :id ::profile/me))]
               ["/cards"
                [
                 ["" (-> (card/get-cards store)
                         (assoc :id ::card/get-cards))]

                 ["/register"
                  (-> (card/register-physical store mimi user-store)
                      (assoc :id ::card/register-physical))]

                 ["/register-digital" (-> (card/register-digital-cards store)
                                          (assoc :id ::card/register-digital-cards))]

                 ["/" [
                       [
                        ["" :card-id] [
                                       ["" (-> (card/unregister store)(assoc :id ::card/unregister))]
                                       ["/reload" (-> (payment/reload store payment-gateway mimi)(assoc :id ::payment/reload))]
                                       ]
                        ]
                       ]
                  ]
                 ]
                ]



               ["/paymentmethods" [["" (-> (payment/methods store payment-gateway)
                                           (assoc :id ::payment/methods))]
                                   [["/" :payment-method-id] (-> (payment/method-detail store payment-gateway)
                                                                (assoc :id ::payment/method-detail))]]]
               ["/socialprofile/account" (-> (social-profile/account store)
                                             (assoc :id ::social-profile/account))]
               ]]]]
  )

(s/defrecord ApiComponent [store mimi user-store authorizer crypto authenticator payment-gateway api-client-store mailer]
  component/Lifecycle
  (start [component]
    (assoc component :routes (api store mimi  user-store authorizer crypto authenticator payment-gateway api-client-store mailer)))
  (stop [component]
        component))

(defn new-api-component []
  (map->ApiComponent {}))

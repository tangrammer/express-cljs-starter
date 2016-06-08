(ns rebujito.api
  (:require
    [com.stuartsierra.component :as component]
    [schema.core :as s]
    [yada.resource :refer (resource)]
    [rebujito.api
     [util :as util]]
    [rebujito.api.resources
     [payment :as payment]
     [addresses :as addresses]
     [account :as account]
     [card :as card]
     [devices :as devices]
     [login :as login]
     [oauth :as oauth]
     [profile :as profile]
     [rewards :as rewards]
     [social-profile :as social-profile]
     ]))

(defn api [store mimi user-store authorizer crypto authenticator payment-gateway api-client-store mailer app-config]
  ["/" [["health"  (-> {:id :jolin
                        :methods
                                {:get {:consumes [{:media-type #{"application/json"}
                                                   :charset "UTF-8"}]
                                       :response (read-string (slurp (clojure.java.io/resource "VERSION.edn"))) }}}
                               (merge (util/common-resource :meta))
                               (merge {:access-control {}})) ]
        ["account/create" (-> (account/create store mimi user-store crypto authenticator authorizer)
                              (assoc :id ::account/create))]
        ["oauth/token" (->  (oauth/token-resource-owner store user-store authorizer crypto api-client-store)
                           (assoc :id ::oauth/token-resource-owner))]
        ["login/forgot-password" (->  (login/forgot-password mailer authorizer authenticator)
                                     (assoc :id ::login/forgot-password))]
        ["devices/register" (->  (devices/register store authorizer authenticator)
                                (assoc :id ::devices/register))]
        ["me" [
               ["" (-> (account/me store mimi user-store authorizer authenticator app-config)
                       (assoc :id ::account/me))]
               ["/addresses"  (->  (addresses/create user-store authorizer authenticator)
                                (assoc :id ::addresses/create))]
               ["/logout/"  (-> (login/logout user-store authorizer authenticator)
                                (assoc :id ::login/logout))]
               ["/login/validate-password" (-> (login/validate-password user-store crypto authorizer authenticator)
                                               (assoc :id ::login/validate-password))]
               ["/profile"  (-> (profile/me store mimi user-store authorizer authenticator app-config)
                                  (assoc :id ::profile/me))]
               ["/rewards" (-> (rewards/me-rewards store mimi user-store authorizer authenticator)
                                  (assoc :id ::profile/me-rewards))]
               ["/cards"
                [["" (-> (card/get-cards store)
                         (assoc :id ::card/get-cards))]
                 ["/history"
                  (-> (card/history store)
                      (assoc :id ::card/history))]
                 ["/register"
                  (-> (card/register-physical store mimi user-store)
                      (assoc :id ::card/register-physical))]

                 ["/register-digital" (-> (card/register-digital-card store mimi user-store)
                                          (assoc :id ::card/register-digital-cards))]

                 ["/" [
                       [
                        ["" :card-id] [
                                       ["" (-> (card/unregister store) (assoc :id ::card/unregister))]
                                       ["/reload" (-> (card/reload store payment-gateway mimi)
                                                      (assoc :id ::card/reload))]
                                       ]
                        ]
                       ]
                  ]
                 ]
                ]

               ["/devices/register" (-> (devices/register store authorizer authenticator)
                                                 (assoc :id ::devices/me/register))]

               ["/paymentmethods" [["" (-> (payment/methods store payment-gateway authorizer authenticator)
                                           (assoc :id ::payment/methods))]
                                   [["/" :payment-method-id] (-> (payment/method-detail store payment-gateway)
                                                                 (assoc :id ::payment/method-detail))]]]
               ["/socialprofile/account" (-> (social-profile/account store)
                                             (assoc :id ::social-profile/account))]
               ]]]]
  )

(defn dynamic-resource [d]
  (clojure.walk/postwalk
   #(if (:swagger/tags %)
      (do  (log/debug ">> extending resource" (:swagger/tags %))
           (resource (let [data (update % :swagger/tags (fn [c] (conj c :more)))
                           data (if (:consumes data)
                                  data
                                  (assoc data :consumes   [{:media-type #{"application/json"}
                                                            :charset "UTF-8"}]))]
                       data)))
      %) d))

(s/defrecord ApiComponent [app-config store mimi user-store authorizer crypto authenticator payment-gateway api-client-store mailer]
  component/Lifecycle
  (start [component]
    (assoc component :routes (dynamic-resource (api store mimi  user-store authorizer crypto authenticator payment-gateway api-client-store mailer app-config))))
  (stop [component]
        component))

(defn new-api-component [app-config]
  (map->ApiComponent {:app-config app-config}))

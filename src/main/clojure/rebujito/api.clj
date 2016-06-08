(ns rebujito.api
  (:require
    [com.stuartsierra.component :as component]
    [schema.core :as s]
    [yada.resource :refer (resource)]
    [taoensso.timbre :as log]
    [rebujito.scopes :as scopes]
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
                               (merge (util/common-resource :meta))) ]
        ["account/create" (-> (account/create store mimi user-store crypto)
                              (assoc :id ::account/create
                                     :oauth {:post scopes/application}))]
        ["oauth/token" (->  (oauth/token-resource-owner store user-store authorizer crypto api-client-store)
                            (assoc :id ::oauth/token-resource-owner))]
        ["login/forgot-password" (->  (login/forgot-password mailer authorizer)
                                      (assoc :id ::login/forgot-password
                                             :oauth {:post scopes/application}))]
        ["devices/register" (->  (devices/register store)
                                (assoc :id ::devices/register))]
        ["me" [
               ["" (-> (account/me store mimi user-store app-config)
                       (assoc :id ::account/me
                              :oauth {:get scopes/user}))]
               ["/addresses" [
                              ["" (-> (addresses/create user-store)
                                      (assoc :id ::addresses/create
                                              :oauth {:post scopes/user
                                                      ; :get scopes/user
                                                      }))]
                              [["/" :address-uuid] (-> (addresses/get-one user-store)
                                                       (assoc :id ::addresses/get
                                                              :oauth {:get scopes/user}))]]]
                            ;  ]]
               ["/logout/"  (-> (login/logout user-store)
                                (assoc :id ::login/logout))]
               ["/login/validate-password" (-> (login/validate-password user-store crypto authenticator)
                                               (assoc :id ::login/validate-password
                                                      :oauth {:post scopes/user}))]
               ["/profile"  (-> (profile/me store mimi user-store app-config)
                                (assoc :id ::profile/me
                                       :oauth {:get scopes/user}))]
               ["/rewards" (-> (rewards/me-rewards store mimi user-store)
                               (assoc :id ::profile/me-rewards
                                      :oauth {:get scopes/user}))]
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

               ["/devices/register" (-> (devices/register store )
                                                 (assoc :id ::devices/me/register))]

               ["/paymentmethods" [["" (-> (payment/methods store payment-gateway)
                                           (assoc :id ::payment/methods
                                                  :oauth {:get  scopes/user
                                                          :post scopes/user}))]
                                   [["/" :payment-method-id] (-> (payment/method-detail store payment-gateway)
                                                                 (assoc :id ::payment/method-detail))]]]
               ["/socialprofile/account" (-> (social-profile/account store)
                                             (assoc :id ::social-profile/account))]
               ]]]]
  )

(defn dynamic-resource [d authenticator authorizer]
  (clojure.walk/postwalk
   #(if (:swagger/tags %)
      (do  (log/debug ">> extending resource" (:swagger/tags %))
           (resource (let [data %
                           data (if (:oauth data)
                                  (-> data
                                      (merge (util/access-control* authenticator authorizer (:oauth data)))
                                      (dissoc :oauth))
                                  data)
                           data (if (:consumes data)
                                  data
                                  (assoc data :consumes   [{:media-type #{"application/json"}
                                                            :charset "UTF-8"}]))]
                       data)))
      %) d))

(s/defrecord ApiComponent [app-config store mimi user-store authorizer crypto authenticator payment-gateway api-client-store mailer]
  component/Lifecycle
  (start [component]
    (assoc component :routes (dynamic-resource (api store mimi  user-store authorizer crypto authenticator payment-gateway api-client-store mailer app-config) authenticator authorizer)))
  (stop [component]
        component))

(defn new-api-component [app-config]
  (map->ApiComponent {:app-config app-config}))

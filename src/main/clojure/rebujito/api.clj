(ns rebujito.api
  (:require
    [com.stuartsierra.component :as component]
    [schema.core :as s]
    [yada.resource :refer (resource)]
    [yada.yada :as yada]
    [taoensso.timbre :as log]
    [rebujito.scopes :as scopes]
    [plumbing.core :refer (?>)]
    [rebujito.api
     [util :as util]]
    [rebujito.api.resources.content :as content]
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

(defn api [store mimi
           token-store
           user-store
           authorizer
           crypto
           authenticator
           payment-gateway
           api-client-store
           mailer
           app-config
           counter-store]

  [""  [["/health"  (-> {:id :health
                         :methods
                                 {:get {:consumes [{:media-type #{"application/json"}
                                                    :charset "UTF-8"}]
                                        :response (read-string (slurp (clojure.java.io/resource "VERSION.edn"))) }}}
                        (merge (util/common-resource :meta))) ]

        [["/content/sitecore/content/"
            ; FR/3rd Party Mobile Content/iOS-Account/Terms of Use
            :market "/" [#".+" :whatever] "/" [#"iOS-Account(%2F|\/)Terms(%20)of(%20)Use" :mediamonks-weirdness]]
            (yada/handler content/terms-json)]

        [["/settings/" :platform "/" :version "/" :market] (yada/handler content/settings-json)]

        ["/account/create" (-> (account/create store mimi user-store crypto)
                               (assoc :id ::account/create
                                      :oauth {:post scopes/application}))]
        ["/oauth/token" (->  (oauth/token-resource-owner store token-store user-store authenticator authorizer crypto api-client-store)
                             (assoc :id ::oauth/token-resource-owner))]
        ["/login/forgot-password" (-> (login/forgot-password mailer authorizer)
                                      (assoc :id ::login/forgot-password
                                             :oauth {:post scopes/application}))]
        ["/devices/register" (-> (devices/register store)
                                 (assoc :id ::devices/register))]
        ["/me"[
               ["" (-> (account/me store mimi user-store app-config)
                       (assoc :id ::account/me
                              :oauth {:get scopes/user}))]
               ["/addresses" [
                              ["" (-> (addresses/addresses user-store)
                                      (assoc :id ::addresses/addresses
                                             :oauth {:post scopes/user
                                                     :get scopes/user}))]
                              [["/" :address-id] (-> (addresses/get-one user-store)
                                                       (assoc :id ::addresses/get
                                                              :oauth {:delete scopes/user
                                                                      :get scopes/user}))]]]
               ["/logout"  (-> (login/logout user-store token-store)
                               (assoc :id ::login/logout
                                      :oauth {:get scopes/user}))]
               ["/logout/"  (-> (login/logout user-store token-store)
                                (assoc :id ::login/logout
                                       :oauth {:get scopes/user}))]
               ["/login/validate-password" (-> (login/validate-password user-store crypto authenticator)
                                               (assoc :id ::login/validate-password
                                                      :oauth {:post scopes/user}))]
               ["/profile"  (-> (profile/profile store mimi user-store app-config)
                                (assoc :id ::profile/me
                                       :oauth {:get scopes/user}))]
               ["/rewards" (-> (rewards/me-rewards store mimi user-store)
                               (assoc :id ::profile/me-rewards
                                      :oauth {:get scopes/user}))]
               ["/cards"
                [["" (-> (card/cards user-store mimi)
                         (assoc :id ::card/get-cards
                                :oauth {:get scopes/user}))]
                 ["/history"
                  (-> (card/history user-store mimi)
                      (assoc :id ::card/history))]
                 ["/register"
                  (-> (card/register-physical user-store mimi)
                      (assoc :id ::card/register-physical
                             :oauth {:post scopes/user}))]

                 ["/register-digital" (-> (card/register-digital-card user-store mimi counter-store)
                                          (assoc :id ::card/register-digital-cards
                                                 :oauth {:post scopes/user}))]

                 ["/" [
                       [
                        ["" :card-id] [
                                       ["" (-> (card/card user-store mimi) (assoc :id ::card/card))]
                                       ["/reload" (-> (card/reload user-store mimi payment-gateway app-config)
                                                      (assoc :id ::card/reload
                                                             :oauth {:post scopes/user}))]
                                       ["/autoreload" (-> (card/autoreload user-store mimi payment-gateway app-config)
                                                          (assoc :id ::card/autoreload
                                                                 :oauth {:post scopes/user}))]
                                       ["/autoreload/disable" (-> (card/autoreload-disable user-store mimi payment-gateway app-config)
                                                                  (assoc :id ::card/autoreload-disable
                                                                         :oauth {:put scopes/user}))]


                                       ["/balance" (-> (card/balance user-store mimi app-config)
                                                       (assoc :id ::card/balance
                                                              :oauth {:get scopes/user}))]
                                       ["/balance-realtime" (-> (card/balance user-store mimi app-config)
                                                                (assoc :id ::card/balance-realtime
                                                                       :oauth {:get scopes/user}))]
                                       ]]]]
                ]
               ]

               ["/devices" [
                            ["/register" (-> (devices/register store )
                                             (assoc :id ::devices/me/register
                                                    :oauth {:post scopes/user}))]
                            ["/reporting/report" (-> (devices/report store)
                                                     (assoc :id ::devices/me/report
                                                            :oauth {:post scopes/user}))]
               ]]

               ["/paymentmethods" [["" (-> (payment/methods user-store payment-gateway)
                                           (assoc :id ::payment/methods
                                                  :oauth {:get  scopes/user
                                                          :post scopes/user}))]
                                   [["/" :payment-method-id] (-> (payment/method-detail user-store store payment-gateway)
                                                                 (assoc :id ::payment/method-detail
                                                                        :oauth {:delete  scopes/user
                                                                                :get  scopes/user
                                                                                :put scopes/user
                                                                                :post scopes/user}))]]]
               ["/socialprofile/account" (-> (social-profile/account store)
                                             (assoc :id ::social-profile/account))]
               ]]]]
  )

(defn dynamic-resource [d authenticator authorizer]
  (clojure.walk/postwalk
   #(if (:swagger/tags %)
      (do  (log/debug ">> extending resource" (:swagger/tags %))
           (resource (let [data %
                           data (-> data
                                    (?> (:oauth data)
                                        (->
                                         (merge (util/access-control* authenticator authorizer (:oauth data)))
                                         (dissoc :oauth)))
                                    (?> (nil? (:consumes data))
                                        (assoc  :consumes [{:media-type #{"application/json"} :charset "UTF-8"}]))
                                    (?> (-> data :methods :post :parameters :body)
                                        (update-in [:methods :post :parameters :body] merge util/optional-risk))
                                    (?> (-> data :methods :put :parameters :body)
                                        (update-in [:methods :put :parameters :body] merge util/optional-risk))
                                    (?> (-> data :methods :delete :parameters :body)
                                        (update-in [:methods :delete :parameters :body] merge util/optional-risk))

                                    )]
                       data)))
      %) d))

(s/defrecord ApiComponent [app-config store mimi token-store user-store authorizer crypto authenticator payment-gateway api-client-store mailer counter-store]
  component/Lifecycle
  (start [component]
    (assoc component :routes (dynamic-resource (api store mimi token-store user-store authorizer crypto authenticator payment-gateway api-client-store mailer app-config counter-store) authenticator authorizer)))
  (stop [component]
        component))

(defn new-api-component [app-config]
  (map->ApiComponent {:app-config app-config}))

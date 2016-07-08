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
     [customer-admin :as customer-admin]
     ]
    [rebujito.api.resources.card
     [reload :as card-reload]]))

(defn- rewrite-methods [resource]
 (let [delete (-> resource :methods :delete)]
   (assoc resource :methods {:post delete})))

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
           counter-store
           webhook-store]

  [""  [[["/check-reload/" :card-number]  (-> (card-reload/check user-store mimi payment-gateway app-config mailer webhook-store)
                                                (assoc :id ::card-reload/check))]
        ["/health"  (-> {:id :health
                         :methods
                                 {:get {:consumes [{:media-type #{"application/json"}
                                                    :charset "UTF-8"}]
                                        :response (read-string (slurp (clojure.java.io/resource "VERSION.edn"))) }}}
                        (merge (util/common-resource :meta))) ]

        [["/content/sitecore/content/"
            ; FR/3rd Party Mobile Content/iOS-Account/Terms of Use
            :market "/" [#".+" :whatever] "/" [#"iOS-Account(%2F|\/)Terms(%20)of(%20)Use" :mediamonks-weirdness]]
         (-> (yada/handler content/terms-json)
             (assoc :id ::content/terms-json)
             )]

        [["/settings/" :platform "/" :version "/" :market] (yada/handler content/settings-json)]

        ["/account/create" (-> (account/create store mimi user-store crypto mailer authorizer app-config)
                               (assoc :id ::account/create
                                      :oauth {:post scopes/application}))]
        ["/oauth/token" (->  (oauth/token-resource-owner token-store user-store authenticator authorizer crypto api-client-store app-config)
                             (assoc :id ::oauth/token-resource-owner))]

        ["/login/verify-email" (-> (login/verify-email authorizer user-store)
                                   (assoc :id ::login/verify-email
                                          :oauth {:put scopes/verify-email}
                                          :check-valid-token-store true
                                          ))]

        ["/login/forgot-password" (-> (login/forgot-password user-store mailer authenticator authorizer app-config)
                                      (assoc :id ::login/forgot-password
                                             :oauth {:post scopes/application}))]

        ;; this is the call after we try reset-password and click in link-token from email
        ["/login/set-new-password" (-> (login/set-new-password user-store crypto authorizer)
                                       (assoc :id ::login/set-new-password
                                              :oauth {:put scopes/reset-password}
                                              :check-valid-token-store true))]

        ["/login/change-email" (-> (login/change-email authorizer authenticator user-store token-store)
                                      (assoc :id ::login/change-email
                                             :oauth {:put scopes/change-email}
                                             :check-valid-token-store true))]
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
                                                              :oauth {:put scopes/user
                                                                      :delete scopes/user
                                                                      :get scopes/user}))]]]
               ["/logout"  (-> (login/logout authorizer)
                               (assoc :id ::login/logout
                                      :oauth {:get scopes/user}))]

               ["/logout/"  (-> (login/logout authorizer)
                                (assoc :id ::login/logout
                                       :oauth {:get scopes/user}))]
               ["/login/change-email" (-> (login/me-change-email user-store crypto authenticator authorizer mailer app-config)
                                          (assoc :id ::login/me-change-email
                                                 :oauth {:post scopes/user}))]
               ["/login/validate-password" (-> (login/validate-password user-store crypto authenticator)
                                               (assoc :id ::login/validate-password
                                                      :oauth {:post scopes/user}))]

               ["/login/change-password" (-> (login/me-change-password authorizer authenticator user-store crypto mailer)
                                             (assoc :id ::login/me-change-password
                                             :oauth {:post scopes/user}))]

               ;; deprecacted
               ;; TODO check with Marcing if we can remove "login/reset-usernamex"
               #_["/login/reset-username" (-> (login/reset-username authorizer authenticator user-store crypto mailer)
                                             (assoc :id ::login/reset-username
                                             :oauth {:post scopes/user}))]
               ["/profile"  (-> (profile/profile mimi user-store app-config)
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
                  (-> (card/history-resource user-store mimi)
                      (assoc :id ::card/history
                             :oauth {:get scopes/user}))]
                 ["/register"
                  (-> (card/register-physical user-store mimi)
                      (assoc :id ::card/register-physical
                             :oauth {:post scopes/user}))]

                 ["/register-digital" (-> (card/register-digital-card user-store mimi counter-store)
                                          (assoc :id ::card/register-digital-cards
                                                 :oauth {:post scopes/user}))]

                 ["/transfer/from" (-> (card/transfer-from user-store mimi)
                                       (assoc :id ::card/transfer
                                              :oauth {:post scopes/user}))]

                 ["/transfer/to" (-> (card/transfer-to user-store mimi)
                                     (assoc :id ::card/transfer
                                            :oauth {:post scopes/user}))]

                 ["/" [
                       [
                        ["" :card-id] [
                                       ["" (-> (card/card user-store mimi) (assoc :id ::card/card :oauth {:get scopes/user}))]
                                       ["/reload" (-> (card-reload/reload user-store mimi payment-gateway app-config)
                                                      (assoc :id ::card-reload/reload
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
                                   [["/" :payment-method-id] [["" (-> (payment/method-detail user-store store payment-gateway)
                                                                      (assoc :id ::payment/method-detail
                                                                             :oauth {:delete scopes/user
                                                                                     :get scopes/user
                                                                                     :put scopes/user}))]
                                                               ["/delete" (-> (payment/method-detail user-store store payment-gateway)
                                                                              (rewrite-methods)
                                                                              (assoc :id ::payment/delete-method-detail
                                                                                     :oauth {:post scopes/user}))]]
                                   ]]]
               ["/socialprofile/account" (-> (social-profile/account user-store)
                                             (assoc :id ::social-profile/account
                                                    :oauth {:put scopes/user}))]
               ]]
        ;; customer-admin

        [["/users/" :user-id]
         [["" (-> (customer-admin/user user-store mimi)
                  (assoc :id ::customer-admin/user
                         :oauth {:delete scopes/customer-admin}))]
          [["/addresses/" :address-id] (-> (customer-admin/address user-store)
                                           (assoc :id ::customer-admin/address
                                                  :oauth {:put scopes/customer-admin}))]
          ["/forgot-password" (-> (customer-admin/forgot-password user-store mailer authenticator authorizer app-config)
                                  (assoc :id ::customer-admin/forgot-password
                                         :oauth {:post scopes/customer-admin}))]

          ["/transfer" [["/to" (-> (customer-admin/transfer-to mimi user-store)
                                   (assoc :id ::customer-admin/transfer-to
                                          :oauth {:post scopes/customer-admin}))]
                        ["/from" (-> (customer-admin/transfer-from mimi user-store)
                                     (assoc :id ::customer-admin/transfer-from
                                            :oauth {:post scopes/customer-admin}))]
                        ["/to-new-digital" (-> (customer-admin/transfer-to-new-digital mimi user-store counter-store)
                                             (assoc :id ::customer-admin/transfer-to-new-digital
                                                    :oauth {:post scopes/customer-admin}))]]]

          ["/profile"  (-> (customer-admin/profile mimi user-store app-config)
                           (assoc :id ::customer-admin/profile
                                  :oauth {:get scopes/customer-admin}))]
          ["/add-stars"  (-> (customer-admin/add-stars mimi user-store app-config)
                             (assoc :id ::customer-admin/add-stars
                                    :oauth {:put scopes/customer-admin}))]
          ["/cards/history" (-> (customer-admin/history user-store mimi)
                                (assoc :id ::customer-admin/history
                                       :oauth {:get scopes/customer-admin}))]
         ]]

        ["/search-customer" (-> (customer-admin/search mimi user-store app-config)
                                (assoc :id ::customer-admin/search
                                       :oauth {:get scopes/customer-admin}))]
        ]]
  )

(defn dynamic-resource [d authenticator authorizer token-store]
  (clojure.walk/postwalk
   #(if (:swagger/tags %)
      (do  (log/debug ">> extending resource" (:swagger/tags %))
           (resource (let [data %
                           data (-> data
                                    (?> (:oauth data)
                                        (->
                                         (merge (util/access-control* authenticator authorizer (:oauth data) (:check-valid-token-store data)  token-store))
                                         (dissoc :oauth)))
                                    (?> (nil? (:consumes data))
                                        (assoc  :consumes [{:media-type #{"application/json"} :charset "UTF-8"}]))
                                    (?> (-> data :methods :post :parameters :body)
                                        (update-in [:methods :post :parameters :body] merge util/optional-risk))
                                    (?> (-> data :methods :put :parameters :body)
                                        (update-in [:methods :put :parameters :body] merge util/optional-risk))
                                    (?> (-> data :methods :delete :parameters :body)
                                        (update-in [:methods :delete :parameters :body] merge util/optional-risk))
                                    (dissoc :check-valid-token-store)

                                    )]
                       data)))
      %) d))

(s/defrecord ApiComponent [app-config store mimi token-store user-store authorizer crypto authenticator payment-gateway api-client-store mailer counter-store webhook-store]
  component/Lifecycle
  (start [component]
    (assoc component :routes (dynamic-resource (api store mimi token-store user-store authorizer crypto authenticator payment-gateway api-client-store mailer app-config counter-store webhook-store) authenticator authorizer token-store)))
  (stop [component]
        component))

(defn new-api-component [app-config]
  (map->ApiComponent {:app-config app-config}))

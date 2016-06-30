(ns rebujito.api.resources.card.reload
  (:require
   [taoensso.timbre :as log]
   [manifold.deferred :as d]
   [rebujito.api.resources :refer (domain-exception)]
   [rebujito.protocols :as p]
   [rebujito.schemas :refer (AutoReloadMongo)]
   [rebujito.api.util :as util]
   [rebujito.util :refer (dtry dcatch error*)]
   [rebujito.mongo :refer [id>mimi-id]]
   [rebujito.store.mocks :as mocks]
   [monger.operators :refer [$push]]
   [cheshire.core :as json]
   [schema.core :as s]
   [yada.resource :refer [resource]]))


(def schema {:reload {:post {:amount Long
                             :paymentMethodId String
                             (s/optional-key :acceptTerms) Boolean
                             (s/optional-key :expirationYear) Long
                             (s/optional-key :expirationMonth) Long
                             (s/optional-key :sessionId) String}}})

(defn check [user-store mimi payment-gateway app-config mailer]
  (->
   {:methods
    {:get {:parameters {:path {:card-number String}}
           :response (fn [ctx]
                       (dcatch ctx
                               (d/let-flow [card-number (-> ctx :parameters :path :card-number)

                                            {:keys [user card]} (p/get-user-and-card user-store card-number)

                                            autoreload-profile (-> card :autoReloadProfile )

                                            enabled? (:status autoreload-profile)]

                                           (if enabled?
                                             (d/let-flow [balances (when (:cardNumber card)
                                                                     (p/balances mimi (:cardNumber card)))

                                                          current-balance (->> (-> balances :programs)
                                                                               (filter (fn [{:keys [code]}]
                                                                                         (= "SGC001" code)))
                                                                               first
                                                                               :balance)

                                                          autoreload-threshold-amount (:triggerAmount autoreload-profile)

                                                          balance-below-auto-reload-threshold (<= current-balance autoreload-threshold-amount)]

                                                         (if balance-below-auto-reload-threshold

                                                           (d/let-flow [payment-method-data (first (filter (fn [{:keys [paymentMethodId]}]
                                                                                                             (= paymentMethodId (:paymentMethodId autoreload-profile)))
                                                                                                           (:paymentMethods user)))

                                                                        payment-data (-> (p/execute-payment
                                                                                          payment-gateway
                                                                                          (merge (select-keys user [:emailAddress :lastName :firstName])
                                                                                                 {:amount  (long (:amount autoreload-profile))
                                                                                                  :currency (:currency-code app-config)
                                                                                                  :cvn "123" ;; TODO how can i find this value?
                                                                                                  :routingNumber (-> payment-method-data :routingNumber)
                                                                                                  :transactionId "12345" ;; todo how can I find this value?
                                                                                                  }))
                                                                                         (d/catch clojure.lang.ExceptionInfo
                                                                                             (fn [e]
                                                                                               (p/send mailer {:to [(:emailAddress user) (:admin-contact app-config)]
                                                                                                               :subject "Error payment! "
                                                                                                               :content "ups!!!!"})
                                                                                               (manifold.deferred/error-deferred e))))

                                                                        mimi-card-data (when payment-data
                                                                                         (-> (p/load-card mimi card-number (:amount autoreload-profile))
                                                                                             (d/catch clojure.lang.ExceptionInfo
                                                                                                 (fn [e]
                                                                                                   (p/send mailer {:to [(:emailAddress user) (:admin-contact app-config)]
                                                                                                                   :subject "Error mimi! "
                                                                                                                   :content "ups!!!!"})
                                                                                                   (manifold.deferred/error-deferred e)))))

                                                                        send-mail (when (and payment-data mimi-card-data)
                                                                                    (p/send mailer {:to (:emailAddress user)
                                                                                                    :subject "A new automatic payment has been done "
                                                                                                    :content (format  "Hello %s ! \n A new payment with this ammount %s has been processed into your starbucks card: %s. \n Your current balance is %s . Enjoy it!"
                                                                                                                      (:firstName user)
                                                                                                                      (:amount autoreload-profile)
                                                                                                                      (:cardId card)
                                                                                                                      (:balance mimi-card-data))}))]

                                                                       (util/>200 ctx {:user user
                                                                                       :mimi-card-data mimi-card-data
                                                                                       :send-mail send-mail
                                                                                       :enabled? enabled?
                                                                                       :balances (:body balances)
                                                                                       :current-balance current-balance
                                                                                       :balance-below-auto-reload-threshold balance-below-auto-reload-threshold
                                                                                       :auto-reload-threshold-amount autoreload-threshold-amount
                                                                                       :card-number card-number
                                                                                       :card card
                                                                                       :payment-data payment-data}))

                                                           (util/>200 ctx nil)
                                                           )
                                                         )
                                             (util/>200 ctx nil)
                                             ))))}}}

   (merge (util/common-resource :me/cards))))



;; TODO: needs to manage all the 400 posibiliites https://admin.swarmloyalty.co.za/sbdocs/docs/starbucks_api/card_management/reload_card.html
(defn reload [user-store mimi payment-gateway app-config]
  (-> {:methods
       {:post {:parameters {:query {:access_token String}
                            :path {:card-id String}
                            :body (-> schema :reload :post)}
               :response (fn [ctx]
                           (let [card-id (-> ctx :parameters :path :card-id)
                                 amount (-> ctx :parameters :body :amount)]
                             (dcatch ctx
                              (d/let-flow [profile-data (util/user-profile-data ctx user-store (:sub-market app-config))
                                           user-id (:user-id (util/authenticated-data ctx))
                                           cards (:cards (p/find user-store user-id))
                                           card-data (first (filter #(= (:cardId %) card-id) cards))
                                           _ (log/info ">>>> card-data::::" card-data card-data)
                                           card-number (:cardNumber card-data)
                                           payment-method-data (p/get-payment-method user-store user-id (-> ctx :parameters :body :paymentMethodId))

                                           _ (log/info ">>>> payment-method-data::::" payment-method-data)

                                           payment-data (p/execute-payment
                                                         payment-gateway
                                                         (merge (select-keys profile-data [:emailAddress :lastName :firstName])
                                                                {:amount amount
                                                                 :currency (:currency-code app-config)
                                                                 :cvn "123"
                                                                 :routingNumber (-> payment-method-data :routingNumber)
                                                                 :transactionId "12345"}))
                                           _ (log/info ">>>> payment-data::::" payment-data)
                                           mimi-card-data (p/load-card mimi card-number amount)
                                           _ (log/info "mimi response" mimi-card-data)]

                                          (util/>200 ctx {:balance (:balance mimi-card-data)
                                                          :balanceDate (.toString (java.time.Instant/now))
                                                          :cardId card-id
                                                          :balanceCurrencyCode "ZA"
                                                          :cardNumber card-number})))))}}}

      (merge (util/common-resource :me/cards))))

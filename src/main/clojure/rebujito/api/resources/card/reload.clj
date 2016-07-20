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
   [rebujito.template :as template]
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

(defn check [user-store mimi payment-gateway app-config mailer webhook-store]
  (->
   {:methods
    {:get {:parameters {:path {:card-number String}}
           :response (fn [ctx]
                       (dcatch ctx
                        (let [webhook-uuid (p/webhook-uuid webhook-store (-> ctx :parameters :path :card-number))
                              webhook-state (:state (p/current webhook-store webhook-uuid))]
                          (if (= "error" webhook-state)
                            (util/>200 ctx nil)
                            (if (or (= "done" webhook-state) (= "ready" webhook-state))
                              (do
                                (p/change-state webhook-store webhook-uuid :new)
                                (d/let-flow [
                                             card-number (-> ctx :parameters :path :card-number)

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
                                                                                                (p/change-state webhook-store webhook-uuid :error)
                                                                                                (p/send mailer {:to [(:emailAddress user)]
                                                                                                                :subject "IMPORTANT INFORMATION REGARDING YOUR STARBUCKS CARD SUBSCRIPTION"
                                                                                                                :content-type "text/html"
                                                                                                                :content (template/render-file "templates/email/reload_payment_failed.html" {})})
                                                                                                (manifold.deferred/error-deferred e))))

                                                                         mimi-card-data (when payment-data
                                                                                          (-> (p/increment-balance! mimi card-number (:amount autoreload-profile) :stored-value)
                                                                                              (d/catch clojure.lang.ExceptionInfo
                                                                                                  (fn [e]
                                                                                                    (p/change-state webhook-store webhook-uuid :error)
                                                                                                    (p/send mailer {:to [(:emailAddress user)]
                                                                                                                    :subject "IMPORTANT INFORMATION REGARDING YOUR STARBUCKS CARD SUBSCRIPTION"
                                                                                                                    :content-type "text/html"
                                                                                                                    :content (template/render-file "templates/email/reload_micros_failed.html" {})})
                                                                                                    (manifold.deferred/error-deferred e)))))

                                                                         send-mail (when (and payment-data mimi-card-data)
                                                                                     (p/send mailer {:to [(:emailAddress user)]
                                                                                                     :subject "Confirmation of Starbucks Card Automatic Reload"
                                                                                                     :content-type "text/html"
                                                                                                     :content (template/render-file
                                                                                                               "templates/email/reload_success.html"
                                                                                                               {:amount (:amount autoreload-profile)
                                                                                                                :balance (:balance mimi-card-data)
                                                                                                                :cardNumber (:cardNumber card)
                                                                                                                :address nil})}))
                                                                         _ (log/info send-mail)
                                                                         ]

                                                                        (do
                                                                          (p/change-state webhook-store webhook-uuid :done)
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
                                                                                          :payment-data payment-data})))

                                                            (do
                                                              (p/change-state webhook-store webhook-uuid :done)
                                                              (util/>200 ctx nil))
                                                            )
                                                          )
                                              (do
                                                (p/change-state webhook-store webhook-uuid :done)
                                                (util/>200 ctx nil))
                                              )))
                              (do
                                (p/change-state webhook-store webhook-uuid :done)
                                (util/>200 ctx nil))
                              )))))}}}

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
                                     (d/let-flow [profile-data (dtry (do (util/user-profile-data ctx user-store (:sub-market app-config))))
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
                                                  mimi-card-data (when payment-data
                                                                   (p/increment-balance! mimi card-number amount :stored-value))
                                           _ (log/info "mimi response" mimi-card-data)]

                                          (util/>200 ctx {:balance (:balance mimi-card-data)
                                                          :balanceDate (.toString (java.time.Instant/now))
                                                          :cardId card-id
                                                          :balanceCurrencyCode "ZA"
                                                          :cardNumber card-number})))))}}}

      (merge (util/common-resource :me/cards))))

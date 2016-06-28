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

                                                          autoreload-threshold-amount (:amount autoreload-profile)

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

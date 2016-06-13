(ns rebujito.api.resources.card
  (:require
   [taoensso.timbre :as log]
   [manifold.deferred :as d]
   [rebujito.api.resources :refer (domain-exception)]
   [rebujito.protocols :as p]
   [rebujito.api.util :as util]
   [rebujito.mongo :refer [id>mimi-id]]
   [rebujito.store.mocks :as mocks]
   [monger.operators :refer [$push]]
   [cheshire.core :as json]
   [schema.core :as s]
   [yada.resource :refer [resource]]))

(def schema {:register-physical {:post {:cardNumber String}
                                       :pin String}
             :reload {:post {:amount Long
                             :paymentMethodId String
                             (s/optional-key :risk) s/Any
                             (s/optional-key :acceptTerms) Boolean
                             (s/optional-key :expirationYear) Long
                             (s/optional-key :expirationMonth) Long
                             (s/optional-key :sessionId) String}}})

(defn get-next-card-number [counter-store]
  (str (p/increment! counter-store :digital-card-number)))

(defn insert-card! [user-store user-id card]
  (let [card-id (str (java.util.UUID/randomUUID))
        card (assoc card :cardId card-id)]
    (p/update-by-id! user-store user-id {$push {:cards card}})
    card-id))

(defn new-physical-card [data]
  (merge data {:digital false}))

(defn new-digital-card [data]
  (merge data {:digital true}))

(defn dummy-card-data []
  {
   ;;:cardNumber nil
   ;;:cardId nil
   ;; :balance 0
   :primary true
   :cardCurrency "ZAR"
   :nickname "My Card"
   :type "Standard"
   :actions ["Reload"]
   :submarketCode "ZA"
   :balanceDate (.toString (java.time.Instant/now))
   :balanceCurrencyCode "ZAR"})

(def stored-value-program "Starbucks Card")

(defn get-points [mimi card-number]
  (d/let-flow [rewards (p/rewards mimi card-number)
               program (first (filter #(= (:program %) stored-value-program) (:programs rewards)))]
    (:balance program)))

(defn get-card [user-store user-id mimi]
  (d/let-flow [card-data (:cards (p/find user-store user-id))
               card-data (first card-data)
               balance (get-points mimi (:cardNumber card-data))]
    (merge (dummy-card-data) card-data {:balance balance})))

(defn cards [user-store mimi]
  (->
   {:methods
    {:get {:parameters {:query {:access_token String}}
           :response (fn [ctx]
                       (-> (d/let-flow [user-id (:_id (util/authenticated-user ctx))
                                        card-data (get-card user-store user-id mimi)]
                             (util/>200 ctx [card-data]))
                           (d/catch clojure.lang.ExceptionInfo
                               (fn [exception-info]
                                 (domain-exception ctx (ex-data  exception-info))))
                       ))}}}
   (merge (util/common-resource :me/cards))))

(defn card [user-store mimi]
  (->
   {:methods
    {:get {:parameters {:path {:card-id String}
                        :query {:access_token String}}
           :response (fn [ctx]
                       (-> (d/let-flow [user-id (:_id (util/authenticated-user ctx))
                                        card-data (get-card user-store user-id mimi)]
                             (util/>200 ctx card-data))))}
     :delete {:parameters {:path {:card-id String}
                           :query {:access_token String}}
              :response (fn [ctx]
                          (condp = (get-in ctx [:parameters :query :access_token])
                            "500"    (util/>500 ctx ["Internal Server Error :( " "An unexpected error occurred processing the request."])
                            "403"    (util/>403 ctx ["Forbidden" "You have not been granted permission to access the requested method or object."])
                            "121032" (util/>403 ctx ["Card is reported lost or stolen" ""])
                            "121037" (util/>403 ctx ["Card is closed." ""])
                            "404"    (util/>404 ctx ["Not Found" "Resource was not found"])
                            "121018" (util/>400 ctx ["Cannot unregister a digital card that has a balance greater than zero." "Only zero balance digital cards can be unregistered"])
                            (util/>200 ctx ["OK" "Success"])))}}}

   (merge (util/common-resource :me/cards))))

(defn register-physical [user-store mimi]
  (->
   {:methods
    {:post {:parameters {:query {:access_token String}
                         :body (-> schema :register-physical :post)}
            :response (fn [ctx]
                        (-> (d/let-flow [card-number (-> ctx :parameters :body :cardNumber)
                                         auth-user (util/authenticated-user ctx)
                                         user-id (:_id auth-user)
                                         mimi-res (p/register-physical-card mimi {:cardNumber card-number
                                                                                  :customerId (id>mimi-id user-id)})
                                         card (new-physical-card {:cardNumber card-number})
                                         card-id (insert-card! user-store user-id card)]
                              (util/>200 ctx (merge mocks/card (dummy-card-data) (assoc card :cardId card-id))))

                            (d/catch clojure.lang.ExceptionInfo
                                (fn [exception-info]
                                  (domain-exception ctx (ex-data  exception-info))))))}}}

   (merge (util/common-resource :me/cards))))

(defn register-digital-card [user-store mimi counter-store]
  (->
   {:methods
    {:post {:parameters {:query {:access_token String}}
            :response (fn [ctx]
                        (-> (d/let-flow [card-number (get-next-card-number counter-store)
                                         auth-user (util/authenticated-user ctx)
                                         user-id (:_id auth-user)
                                         mimi-res (p/register-physical-card mimi {:cardNumber card-number
                                                                                  :customerId  (id>mimi-id user-id)})
                                         card (new-digital-card {:cardNumber card-number})
                                         card-id (insert-card! user-store user-id card)]
                              (util/>200 ctx (merge mocks/card (dummy-card-data) (assoc card :cardId card-id))))

                            (d/catch clojure.lang.ExceptionInfo
                                (fn [exception-info]
                                  (domain-exception ctx (ex-data  exception-info))))))}}}

   (merge (util/common-resource :me/cards))))

(def empty-history {:paging {:total 0
                             :offset 0
                             :limit 10
                             :returned 0}
                    :historyItems []})

(defn history [user-store mimi]
  (->
   {:methods
    {:get {:parameters {:query {:access_token String
                                (s/optional-key :limit) String
                                (s/optional-key :offset) String
                                }}
           :response (fn [ctx]
                       (util/>200 ctx empty-history))}}}

   (merge (util/common-resource :me/cards))))

(defn reload [user-store mimi payment-gateway app-config]
  (-> {:methods
       {:post {:parameters {:query {:access_token String}
                            :path {:card-id String}
                            :body (-> schema :reload :post)}
               :response (fn [ctx]
                           (->
                            (d/let-flow [profile-data (util/user-profile-data ctx user-store (:sub-market app-config))
                                         auth-user (util/authenticated-user ctx)
                                         payment-method-data (first (filter #(= (:paymentMethodId %)
                                                                          (-> ctx :parameters :body :paymentMethodId))
                                                                            (:paymentMethods (p/find user-store (:_id auth-user) ))))
                                         _ (log/info ">>>> payment-method-data::::" payment-method-data)

                                         payment-data (p/execute-payment
                                                       payment-gateway
                                                       (merge (select-keys profile-data [:emailAddress :lastName :firstName])
                                                              {:amount (-> ctx :parameters :body :amount)
                                                               :currency (:currency-code app-config)
                                                               :cvn "123"
                                                               :routingNumber (-> payment-method-data :routingNumber)
                                                               :transactionId "12345"}))
                                         _ (log/info ">>>> payment-data::::" payment-data)
                                         mimi-card-data (p/load-card mimi (-> ctx :parameters :body :card-id)
                                                                     (-> ctx :parameters :body :amount))]
                                        (util/>200 ctx (-> (select-keys mimi-card-data [:balance :balanceDate ])
                                                           (assoc :cardId nil #_(-> ctx :parameters :body :card-id)
                                                                  :balanceCurrencyCode (:currency mimi-card-data)
                                                                  :cardNumber "7777064158671182" ;; TODO: ASK AHOUT card-number
                                                                  )
                                                           )))
                            (d/catch clojure.lang.ExceptionInfo
                                (fn [exception-info]
                                  (domain-exception ctx (ex-data exception-info))))))}}}

      (merge (util/common-resource :me/cards))))

(defn balance [user-store mimi]
  (-> {:methods
       {:get {:parameters {:query {:access_token String}
                           :path {:card-id String}}
              :response (fn [ctx]
                         (d/let-flow [user-id (:_id (util/authenticated-user ctx))
                                      card (get-card user-store user-id mimi)]
                           (util/>200 ctx {:cardId (:cardId card)
                                           :cardNumber (:cardNumber card)
                                           :balance (:balance card)
                                           :balanceDate (.toString (java.time.Instant/now))
                                           :balanceCurrencyCode "ZAR"})))
             }}}
   (merge (util/common-resource :me/cards))))

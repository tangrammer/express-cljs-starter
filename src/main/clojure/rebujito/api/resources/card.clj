(ns rebujito.api.resources.card
  (:require
   [taoensso.timbre :as log]
   [manifold.deferred :as d]
   [rebujito.api.resources :refer (domain-exception)]
   [rebujito.protocols :as p]
   [rebujito.schemas :refer (AutoReloadMongo)]
   [rebujito.api.util :as util]
   [rebujito.mongo :refer [id>mimi-id]]
   [rebujito.store.mocks :as mocks]
   [monger.operators :refer [$push]]
   [cheshire.core :as json]
   [schema.core :as s]
   [yada.resource :refer [resource]]))

(def schema {:register-physical {:post {:cardNumber String
                                        :pin String
;                                        :risk s/Any
                                        }
                                 :pin String}
             :reload {:post {:amount Long
                             :paymentMethodId String
;                             (s/optional-key :risk) s/Any
                             (s/optional-key :acceptTerms) Boolean
                             (s/optional-key :expirationYear) Long
                             (s/optional-key :expirationMonth) Long
                             (s/optional-key :sessionId) String}}
             :autoreload {:post {:status (s/enum "active" "disabled")
                                 :autoReloadType (s/enum "Date" "Amount")
                                 :day s/Num
                                 :triggerAmount s/Num
                                 :amount s/Num
                                 :paymentMethodId String}}})


(defn get-next-card-number [counter-store]
  (str (p/increment! counter-store :digital-card-number)))

(defn new-physical-card [data]
  (merge data {:digital false}))

(defn new-digital-card [data]
  (merge data {:digital true}))

(def value-link-arb-class "242")

(defn blank-card-data []
  {:primary true
   :cardCurrency "ZAR"
   :nickname "My Card"
   :type "Standard"
   :actions ["Reload" "AutoReload"]
   :submarketCode "ZA"
   :class value-link-arb-class
   :owner true
   :partner false
   :autoReloadProfile nil
   :balance 0
   :balanceDate (.toString (java.time.Instant/now))
   :balanceCurrencyCode "ZAR"})

(defn get-card [user-store user-id mimi]
  (d/let-flow [card-data (:cards (p/find user-store user-id))
               card-data (first card-data)
               balance (p/get-points mimi (:cardNumber card-data))]
    (when card-data
      (merge
        (select-keys mocks/card [:imageUrls])
        (blank-card-data)
        card-data
        {:balance balance}))))

(defn cards [user-store mimi]
  (->
   {:methods
    {:get {:parameters {:query {:access_token String}}
           :response (fn [ctx]
                       (-> (d/let-flow [user-id (:_id (util/authenticated-user ctx))
                                        card-data (get-card user-store user-id mimi)]
                             (util/>200 ctx (if card-data [card-data] [])))
                           (d/catch clojure.lang.ExceptionInfo
                               (fn [exception-info]
                                 (domain-exception ctx (ex-data  exception-info))))))}}}

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
                                         card-id (p/insert-card! user-store user-id card)]
                              (util/>200 ctx (merge
                                              (select-keys mocks/card [:imageUrls])
                                              (blank-card-data)
                                              (assoc card :cardId card-id))))

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
                                         card-id (p/insert-card! user-store user-id card)]
                              (util/>200 ctx (merge
                                              (select-keys mocks/card [:imageUrls])
                                              (blank-card-data)
                                              (assoc card :cardId card-id))))

                            (d/catch clojure.lang.ExceptionInfo
                                (fn [exception-info]
                                  (domain-exception ctx (ex-data  exception-info))))))}}}

   (merge (util/common-resource :me/cards))))

(def empty-history {:paging {:total 6
                             :offset 0
                             :limit 50
                             :returned 6}

                    :historyItems [
                    {

         :historyId 123124
         :historyType "SvcTransactionWithPoints"
         :cardId "a5ede6d4-a4ae-4ec3-bded-758c863a9874"
         :isoDate "2016-06-16T22:28:59.0000000Z"
         :modifiedDate nil
         :currency "ZAR"
         :localCurrency "ZAR"
         :totalAmount 101.1
         :svcTransaction {
            :checkId nil
            :transactionType "Redemption"
            :isVoid false
            :localizedStoreName "Brokhurstspruit"
            :storeId "00303"
            :storeType "Physical"
            :localDate nil
            :currency "ZAR"
            :localCurrency "ZAR"
            :transactionAmount 0
            :localTransactionAmount 0
            :tax nil
            :newBalance 0
            :description nil
            :tipInfo {
               :tippable false
               :tippableEndDate nil
               :tipTransactionId nil
               :amount nil
               :status "None"
            }
         }
         :localTotalAmount 0
         :points [
            {
               :pointType "Purchases"
               :pointsEarned 2
               :promotionName "Purchase"
               :amount 0
               :currency 0
            }
         ]
         :coupon nil
      }
      {
         :historyId 100119
         :historyType "SvcTransactionWithPoints"
         :cardId "a5ede6d4-a4ae-4ec3-bded-758c863a9874"
         :isoDate "2016-06-16T20:42:37.0000000Z"
         :modifiedDate nil
         :currency "ZAR"
         :localCurrency "ZAR"
         :totalAmount 62.3
         :svcTransaction {
            :checkId nil
            :transactionType "Redemption"
            :isVoid false
            :localizedStoreName "Small Town"
            :storeId "00303"
            :storeType "Physical"
            :localDate nil
            :currency "ZAR"
            :localCurrency "ZAR"
            :transactionAmount 0
            :localTransactionAmount 0
            :tax nil
            :newBalance 0
            :description nil
            :tipInfo {
               :tippable false
               :tippableEndDate nil
               :tipTransactionId nil
               :amount nil
               :status "None"
            }
         }
         :localTotalAmount 6.24
         :points [
            {
               :pointType "Purchases"
               :pointsEarned 1
               :promotionName "Purchase"
               :amount 6.24
               :currency 0
            }
         ]
         :coupon nil
      }
      {
         :historyId 100174
         :historyType "Coupon"
         :cardId nil
         :isoDate "2016-06-16T00:00:00.0000000Z"
         :modifiedDate nil
         :currency nil
         :localCurrency nil
         :totalAmount nil
         :svcTransaction nil
         :localTotalAmount nil
         :points nil
         :coupon {
            :couponCode "BFB"
            :name "BIRTHDAY FREE BEVERAGE US"
            :issueDate nil
            :expirationDate "2016-07-16T10:21:44.0000000Z"
            :allowedRedemptionCount 1
            :voucherType "MSREarnCoupon"
            :status "Active"
            :startDate "2016-06-16T00:00:00.0000000Z"
            :lastRedemptionDate "1904-01-01T00:00:00.0000000Z"
            :redemptionCount 0
            :posCouponCode "593"
            :deliveryMethod "Email"
            :source "Unknown"
         }
      }
      {
         :historyId 100188
         :historyType "Coupon"
         :cardId nil
         :isoDate "2016-06-15T00:00:00.0000000Z"
         :modifiedDate nil
         :currency nil
         :localCurrency nil
         :totalAmount nil
         :svcTransaction nil
         :localTotalAmount nil
         :points nil
         :coupon {
            :couponCode "EFD"
            :name "EARNED FREE DRINK"
            :issueDate "2016-06-14T00:00:00.0000000Z"
            :expirationDate "2016-07-30T09:49:08.0000000Z"
            :allowedRedemptionCount 1
            :voucherType "MSREarnCoupon"
            :status "Available"
            :startDate "2016-06-13T00:00:00.0000000Z"
            :lastRedemptionDate "1904-01-01T00:00:00.0000000Z"
            :redemptionCount 0
            :posCouponCode "594"
            :deliveryMethod "Email"
            :source "Unknown"
         }
      }
      {
         :historyId 100207
         :historyType "SvcTransaction"
         :cardId "85607AFB93D21H"
         :isoDate "2016-06-10T20:28:59.0000000Z"
         :modifiedDate nil
         :currency "ZAR"
         :localCurrency "ZAR"
         :totalAmount 4.6
         :svcTransaction {
            :checkId "a5ede6d4-a4ae-4ec3-bded-758c863a9874"
            :transactionType "Redemption"
            :isVoid false
            :localizedStoreName "Bellevue"
            :storeId "00303"
            :storeType "Physical"
            :localDate "2016-06-09T12:28:59.0000000-07:00"
            :currency "ZAR"
            :localCurrency "ZAR"
            :transactionAmount 4.60
            :localTransactionAmount 4.60
            :tax nil
            :newBalance 249.31
            :description nil
            :tipInfo {
               :tippable false
               :tippableEndDate nil
               :tipTransactionId nil
               :amount nil
               :status "None"
            }
         }
         :localTotalAmount 4.6
         :points [
            {
               :pointType "Purchases"
               :pointsEarned 1
               :promotionName "Purchase"
               :amount 4.6
               :currency 0
            }
         ]
         :coupon nil
      }
      {
         :historyId 100207
         :historyType "Point"
         :cardId "a5ede6d4-a4ae-4ec3-bded-758c863a9874"
         :isoDate "2016-06-11T20:28:59.0000000Z"
         :modifiedDate "2016-06-12T18:05:08.4900000Z"
         :currency "ZAR"
         :localCurrency "ZAR"
         :totalAmount 4.6
         :svcTransaction nil
         :localTotalAmount 4.6
         :points [
            {
               :pointType "Purchases"
               :pointsEarned 1
               :promotionName "Purchase"
               :amount 4.6
               :currency 0
            }
         ]
         :coupon nil
      }

                    ]})

(defn location-tr [mimi-location-id]
  (case mimi-location-id
    "43081" "Test Store"
    "43362" "Head Office Training Store"
    "43361" "Rosebank"
    "43541" "Mall of Africa"
    ""))

(defn mimi-to-rebujito-tx [mimi-tx]
          {:historyId (:id mimi-tx)
           :historyType "SvcTransactionWithPoints"
           :cardId "a5ede6d4-a4ae-4ec3-bded-758c863a9874"
           :isoDate "2016-05-24T14:17:59.567Z"
          ;  :isoDate (:date mimi-tx)
           :modifiedDate nil
           :currency "ZAR"
           :localCurrency "ZAR"
           :totalAmount (:amount mimi-tx)
           :localTotalAmount 0
           :points []
           :coupon nil
           :svcTransaction {
              :checkId (:check mimi-tx)
              :transactionType "Redemption"
              :isVoid false
              :localizedStoreName (-> mimi-tx :location location-tr)
              :storeId (-> mimi-tx :location)
              :storeType "Physical"
              :localDate nil
              :currency "ZAR"
              :localCurrency "ZAR"
              :transactionAmount 0
              :localTransactionAmount 0
              :tax nil
              :newBalance (:balance mimi-tx)
              :description nil
              :tipInfo {}
              ; :tipInfo {
              ;    :tippable false
              ;    :tippableEndDate nil
              ;    :tipTransactionId nil
              ;    :amount nil
              ;    :status "None"
              ; }
           }
          ;  :points [
          ;     {
          ;        :pointType "Purchases"
          ;        :pointsEarned 2
          ;        :promotionName "Purchase"
          ;        :amount 0
          ;        :currency 0
          ;     }
          ;  ]
          })

(defn history [user-store mimi]
  (->
   {:methods
    {:get {:parameters {:query {:access_token String
                                (s/optional-key :limit) String
                                (s/optional-key :offset) String
                                }}
           :response (fn [ctx]
                      (-> (d/let-flow [user-id (:_id (util/authenticated-user ctx))
                                       card-data (:cards (p/find user-store user-id))
                                      ;  card-data-bis (first (filter #(= (:cardId %) card-id) card-data))
                                       card-number (-> card-data first :cardNumber)
                                       card-number "9623570900003"
                                       history-data (p/get-history mimi card-number)
                                       transactions (->> history-data :transactions reverse (take 50))]
                             (util/>200 ctx {:paging {:total (count transactions)
                                                      :returned (count transactions)
                                                      :offset 0
                                                      :limit 50}
                                             :historyItems (map mimi-to-rebujito-tx transactions)})
                          )
                          (d/catch clojure.lang.ExceptionInfo
                              (fn [exception-info]
                                (domain-exception ctx (ex-data exception-info))))
                          )

                      ;  (util/>200 ctx (merge
                      ;                   (select-keys empty-history [:paging])
                      ;                   {:historyItems (map
                      ;                                   (fn [x]
                      ;                                     (mimi-to-rebujito-tx
                      ;                                       {:id x
                      ;                                        :amount x
                      ;                                        :check x
                      ;                                        :location "43541"
                      ;                                        :balance x
                      ;                                        :date "2016-06-12T16:12:23.123Z"
                      ;                                        })
                      ;                                     ) [1 2 3 4 5 6])}
                      ;                   ; (mimi-to-rebujito-tx {})
                      ;                 ))
                                      )}}}
                      ;  (util/>200 ctx empty-history))}}}

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
                             (->
                              (d/let-flow [profile-data (util/user-profile-data ctx user-store (:sub-market app-config))
                                           user-id (:_id (util/authenticated-user ctx))
                                           card-data (:cards (p/find user-store user-id))
                                           card-data-bis (first (filter #(= (:cardId %) card-id) card-data))
                                           _ (log/info ">>>> card-data::::" card-data card-data-bis)
                                           card-number (:cardNumber card-data-bis)
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
                                                          :cardNumber card-number}))
                              (d/catch clojure.lang.ExceptionInfo
                                  (fn [exception-info]
                                    (domain-exception ctx (ex-data exception-info)))))))}}}

      (merge (util/common-resource :me/cards))))

(defn autoreload [user-store mimi payment-gateway app-config]
  (-> {:methods
       {:post {:parameters {:query {:access_token String}
                            :path {:card-id String}
                            :body s/Any}
               :response (fn [ctx]
                           (try

                             (util/validate* (-> schema :autoreload :post :paymentMethodId)  (-> ctx :parameters :body :paymentMethodId) [400 "Please supply a payment method id." "Missing payment method identifier attribute is required"])

                             (util/validate* (-> schema :autoreload :post :autoReloadType)  (-> ctx :parameters :body :autoReloadType) [400 "Please supply an auto reload type." "Missing or invalid auto reload type attribute is required. Type must be set to either 'date' or 'amount'."])


                             (util/validate* (-> schema :autoreload :post :day)  (-> ctx :parameters :body :day) [400 "Please supply an auto reload type." "Missing or invalid auto reload type attribute is required. Type must be set to either 'date' or 'amount'."](fn [v]
                                                                                                                                                                                                                                                                               (if (= "Date" (-> ctx :parameters :body :autoReloadType))
                                                                                                                                                                                                                                                                                   (let [v1  (-> ctx :parameters :body :day)]
                                                                                                                                                                                                                                                                                     (and (> v1 0) (< v1 32)))
                                                                                                                                                                                                                                                                                   true)))

                             (util/validate* (-> schema :autoreload :post :amount)  (-> ctx :parameters :body :amount) [400 "Please supply a trigger amount." "For auto reload of type “Amount”, a valid trigger amount attribute is required."])

                             (util/validate* (-> schema :autoreload :post :amount)  (-> ctx :parameters :body :amount) [400 "Please supply an auto reload amount." "Missing or invalid auto reload amount attribute is required. Amount must be within the range of 10-1000"] (fn [v] (if (= "Amount" (-> ctx :parameters :body :autoReloadType))
                                                                                                                                                                                                                                                                                 (let [v1  (-> ctx :parameters :body :amount)]
                                                                                                                                                                                                                                                                                   (and (> v1 9) (< v1 1001)))
                                                                                                                                                                                                                                                                                 true)))

                             ;; TODO: 400	121033	Invalid operation for card market.
                             ;;       400	121034	Card resource not found to fulfill action.
                             (-> (d/let-flow [auth-user (util/authenticated-user ctx)
                                              payment-method-data (p/get-payment-method user-store (:_id auth-user) (-> ctx :parameters :body :paymentMethodId))
                                              body (-> ctx :parameters :body)
                                              body (if (= (-> body :status ) "enabled") (assoc body :status "active") body)
                                              auto-reload-data (p/add-auto-reload user-store (:_id auth-user)
                                                                                  payment-method-data
                                                                                  (-> (select-keys body (keys AutoReloadMongo))
                                                                                      (assoc  :cardId (-> ctx :parameters :path :card-id))))]

                                             (util/>200 ctx {
                                                             :amount (-> body :amount)
                                                             :autoReloadId (:autoReloadId auto-reload-data)
                                                             :autoReloadType (-> body :autoReloadType)
                                                             :cardId (-> ctx :parameters :path :card-id)
                                                             :day (-> body :day)
                                                             :disableUntilDate nil
                                                             :paymentMethodId (-> body :paymentMethodId)
                                                             :status (-> body :status)
                                                             :stoppedDate nil
                                                             :triggerAmount (-> body :triggerAmount)
                                                             }))
                                 (d/catch clojure.lang.ExceptionInfo
                                     (fn [exception-info]
                                       (domain-exception ctx (ex-data exception-info)))))
                             (catch clojure.lang.ExceptionInfo e (domain-exception ctx (ex-data e)))))}}}

      (merge (util/common-resource :me/cards))))

;; TODO: needs testing in mobile app
(defn autoreload-disable [user-store mimi payment-gateway app-config]
  (-> {:methods
       {:put {:parameters {:query {:access_token String}
                           :path {:card-id String}}
               :response (fn [ctx]
                           (-> (d/let-flow [auth-user (util/authenticated-user ctx)
                                            disable-reload-data (p/disable-auto-reload user-store (:_id auth-user) (-> ctx :parameters :path :card-id) )]

                                           (util/>200 ctx nil))
                               (d/catch clojure.lang.ExceptionInfo
                                   (fn [exception-info]
                                     (domain-exception ctx (ex-data exception-info))))))}}}

      (merge (util/common-resource :me/cards))))

(defn balance [user-store mimi app-config]
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
                                           :balanceCurrencyCode (:currency-code app-config)})))
             }}}
   (merge (util/common-resource :me/cards))))

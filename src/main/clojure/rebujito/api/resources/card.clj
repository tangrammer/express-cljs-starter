(ns rebujito.api.resources.card
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

(def schema {:register-physical {:post {:cardNumber String
                                        :pin String
                                        }
                                 :pin String}

             :autoreload {:post {:status (s/enum "active" "disabled")
                                 :autoReloadType (s/enum "Amount")
                                 (s/optional-key :day) (s/enum nil "")
                                 :triggerAmount s/Num
                                 :amount s/Num
                                 :paymentMethodId String}}})

(defn new-physical-card [data]
  (merge data {:digital false}))

(defn new-digital-card [data]
  (merge data {:digital true}))

(def VALUE_LINK_ARB_CLASS "242")

(def CURRENCY_CODE "ZAR")

(defn blank-card-data []
  {:primary true
   :cardCurrency CURRENCY_CODE
   :nickname "My Card"
   :type "Standard"
   :actions ["Reload" "AutoReload"]
   :submarketCode "ZA"
   :class VALUE_LINK_ARB_CLASS
   :owner true
   :partner false
   :autoReloadProfile nil
   :balance 0
   :balanceDate (.toString (java.time.Instant/now))
   :balanceCurrencyCode CURRENCY_CODE})

(def STORED_VALUE_PROGRAM "Starbucks Card")

(defn- get-points-for [balances]
  (let [program  (first (filter #(= (:program %) STORED_VALUE_PROGRAM) (:programs balances)))]
              (or (:balance program) 0)))

(defn- get-card-data [user-store user-id]
  (comment rebujito.store.mocks/card)
  (let [cards (:cards (p/find user-store user-id))]
    (when (>  (count cards) 1)
      (clj-bugsnag.core/notify
          (Exception. (str "Rebujito data error! There's more than one card for this user: " user-id))
          {:api-key (:key (:bugsnag (rebujito.config/config)))
           :meta {:user-id user-id}
           :environment rebujito.util/*bugsnag-release*
           :user user-id}))
    (first cards)))

(defn >get-card [user-store user-id balances]
  (comment rebujito.store.mocks/card
           rebujito.store.mocks/me-rewards)
  (d/let-flow [card-data (get-card-data user-store user-id)
               balance (get-points-for balances)]
    (when card-data
      (merge
       (if (= "Green" (util/get-tier-name balances))
         mocks/greenImageUrls
         mocks/goldImageUrls)
        (blank-card-data)
        card-data
        {:balance balance}))))



(defn cards [user-store mimi]
  (->
   {:methods
    {:get {:parameters {:query {:access_token String}}
           :response (fn [ctx]
                       (dcatch
                        ctx
                        (d/let-flow [user-id (:user-id (util/authenticated-data ctx))
                                     card-data (get-card-data user-store user-id)
                                     balances (when (:cardNumber card-data)
                                                (p/balances mimi (:cardNumber card-data)))
                                     card (>get-card user-store user-id balances)]
                                    (util/>200 ctx (if card-data [card] [])))))}}}

   (merge (util/common-resource :me/cards))))

(defn card [user-store mimi]
  (->
   {:methods
    {:get {:parameters {:path {:card-id String}
                        :query {:access_token String}}
           :response (fn [ctx]
                       (dcatch ctx
                               (do
                                 (d/let-flow [user-id (:user-id (util/authenticated-data ctx))
                                              card-data (get-card-data user-store user-id)
                                              balances (when (:cardNumber card-data)
                                                        (p/balances mimi (:cardNumber card-data)))

                                             card-data (>get-card user-store user-id balances)]
                                            (util/>200 ctx card-data)))))}}}
   (merge (util/common-resource :me/cards))))

(defn register-physical [user-store mimi]
  (->
   {:methods
    {:post {:parameters {:query {:access_token String}
                         :body (-> schema :register-physical :post)}
            :response (fn [ctx]
                        (-> (d/let-flow [card-number (-> ctx :parameters :body :cardNumber)
                                         auth-data (util/authenticated-data ctx)
                                         user-id (:user-id auth-data)
                                         mimi-res (p/register-physical-card mimi {:cardNumber card-number
                                                                                  :customerId (id>mimi-id user-id)})
                                         increment-balance (when mimi-res
                                                             (p/increment-balance! mimi card-number 50 :loyalty))
                                         card (new-physical-card {:cardNumber card-number})
                                         card-id (p/insert-card! user-store user-id card)]
                                        increment-balance ;; force evaluation of this value
                                        (util/>200 ctx (merge
                                                        mocks/greenImageUrls
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
                        (dcatch ctx
                                (d/let-flow [card-number (str (p/increment! counter-store :digital-card-number))
                                             auth-data (util/authenticated-data ctx)
                                             user-id (:user-id auth-data)
                                             mimi-res (p/register-physical-card mimi {:cardNumber card-number
                                                                                      :customerId  (id>mimi-id user-id)})
                                             increment-balance (when mimi-res
                                                                 (p/increment-balance! mimi card-number 50 :loyalty))
                                             card (new-digital-card {:cardNumber card-number})
                                             card-id (p/insert-card! user-store user-id card)]
                                             increment-balance ;; force evaluation of this value
                                             (util/>200 ctx (merge
                                                             mocks/greenImageUrls
                                                            (blank-card-data)
                                                            (assoc card :cardId card-id))))))}}}

   (merge (util/common-resource :me/cards))))

(defn- location-tr [mimi-location-id]
  (case mimi-location-id
    "42581" "QSR Restaurant"
    "43081" "Starbucks Test Store"
    "43361" "Rosebank - RB"
    "43362" "Head Office - Training"
    "43541" "Mall of Africa"
    "44921" "Head Office - Test Store"
    ""))

(defn- mimi-to-rebujito-tx [mimi-tx]
  {:historyId (:id mimi-tx)
   :historyType "SvcTransactionWithPoints"
   :cardId "a5ede6d4-a4ae-4ec3-bded-758c863a9874"
   :isoDate (:date mimi-tx)
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
   }
  })

(defn history [user-store mimi]
  (->
   {:methods
    {:get {:parameters {:query {:access_token String
                                (s/optional-key :limit) String
                                (s/optional-key :offset) String
                                }}
           :response (fn [ctx]
                      (-> (d/let-flow [user-id (:user-id (util/authenticated-data ctx))
                                       card-data (:cards (p/find user-store user-id))
                                       card-number (-> card-data first :cardNumber)
                                       ; some numbers to test history
                                      ;  card-number "9623570800007"
                                      ;  card-number "9623570800099"
                                      ;  card-number "9623570900003"
                                      ;  card-number "9623570900005"
                                      ;  card-number "9623570900007"
                                      ;  card-number "9623570900010"
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

                     )}}}

   (merge (util/common-resource :me/cards))))

(defn autoreload [user-store mimi payment-gateway app-config]
  (-> {:methods
       {:post {:parameters {:query {:access_token String}
                            :path {:card-id String}
                            :body s/Any}
               :response (fn [ctx]
                           (try

                             (util/validate* (-> schema :autoreload :post :paymentMethodId)  (-> ctx :parameters :body :paymentMethodId) [400 "Please supply a payment method id." "Missing payment method identifier attribute is required"])

                             (util/validate* (-> schema :autoreload :post :autoReloadType)  (-> ctx :parameters :body :autoReloadType) [400 "autoReloadType must be set to 'Amount'."])

                             (util/validate* (-> schema :autoreload :post :triggerAmount)  (-> ctx :parameters :body :triggerAmount) [400 "Please supply a trigger amount." "For auto reload of type 'Amount', a valid trigger amount attribute is required."])

                             (util/validate* (-> schema :autoreload :post :amount)  (-> ctx :parameters :body :amount) [400 "Please supply an auto reload amount." "Missing or invalid auto reload amount attribute is required. Amount must be within the range of 10-1000"] (fn [v] (if (= "Amount" (-> ctx :parameters :body :autoReloadType))
                                                                                                                                                                                                                                                                                 (let [v1  (-> ctx :parameters :body :amount)]
                                                                                                                                                                                                                                                                                   (and (> v1 9) (< v1 1001)))
                                                                                                                                                                                                                                                                                 true)))

                             ;; TODO: 400	121033	Invalid operation for card market.
                             ;;       400	121034	Card resource not found to fulfill action.
                             (-> (d/let-flow [auth-data (util/authenticated-data ctx)
                                              payment-method-data (p/get-payment-method user-store (:user-id auth-data) (-> ctx :parameters :body :paymentMethodId))
                                              body (-> ctx :parameters :body)
                                              body (if (= (-> body :status ) "enabled") (assoc body :status "active") body)
                                              auto-reload-data (when payment-method-data
                                                                 (p/add-autoreload-profile-card user-store (:user-id auth-data)
                                                                                                (-> (select-keys body (keys AutoReloadMongo))
                                                                                                    (assoc  :cardId (-> ctx :parameters :path :card-id)))))]

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
                           (-> (d/let-flow [auth-data (util/authenticated-data ctx)
                                            disable-reload-data (p/disable-auto-reload user-store (:user-id auth-data) (-> ctx :parameters :path :card-id) )]

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
                          (d/let-flow [user-id (:user-id (util/authenticated-data ctx))
                                       card-data (get-card-data user-store user-id)
                                       balances (when (:cardNumber card-data)
                                                   (p/balances mimi (:cardNumber card-data)))
                                       card (>get-card user-store user-id balances)]
                           (util/>200 ctx {:cardId (:cardId card)
                                           :cardNumber (:cardNumber card)
                                           :balance (:balance card)
                                           :balanceDate (.toString (java.time.Instant/now))
                                           :balanceCurrencyCode (:currency-code app-config)})))
             }}}
   (merge (util/common-resource :me/cards))))

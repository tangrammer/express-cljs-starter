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
  {:cardNumber nil
   :cardId nil
   :balance 0
   :primary false
   :cardCurrency "ZAR"
   :nickname "My Card"
   :type "Standard"
   :actions ["Reload"]
   :submarketCode "ZA"
   :balanceDate (.toString (java.time.Instant/now))
   :balanceCurrencyCode "ZA"})

(defn get-cards [store]
  (->
   {:methods
    {:get {:parameters {:query {:access_token String}}
           :consumes [{:media-type #{"application/json"}
                       :charset "UTF-8"}]
           :response (fn [ctx]
                       (util/>200 ctx []))}}}
   (merge (util/common-resource :me/cards))))

(defn unregister [store]
  (->
   {:methods
    {:delete {:parameters {:path {:card-id String}
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

(defn register-physical [store mimi user-store]
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
                                         card-id (insert-card! user-store user-id card)
                                         card (assoc card :cardId card-id)]
                              (util/>200 ctx (merge mocks/card (dummy-card-data) card)))

                            (d/catch clojure.lang.ExceptionInfo
                                (fn [exception-info]
                                  (domain-exception ctx (ex-data  exception-info))))))}}}

   (merge (util/common-resource :me/cards))))

(defn register-digital-card [store mimi user-store counter-store]
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
                                         card-id (insert-card! user-store user-id card)
                                         card (assoc card :cardId card-id)]
                              (util/>200 ctx (merge mocks/card (dummy-card-data) card)))

                            (d/catch clojure.lang.ExceptionInfo
                                (fn [exception-info]
                                  (domain-exception ctx (ex-data  exception-info))))))}}}

   (merge (util/common-resource :me/cards))))

(def empty-history {:paging {:total 0
                             :offset 0
                             :limit 10
                             :returned 0}
                    :historyItems []})

(defn history [store]
  (->
   {:methods
    {:get {:parameters {:query {:access_token String
                                (s/optional-key :limit) String
                                (s/optional-key :offset) String
                                }}
           :response (fn [ctx]
                       (util/>200 ctx empty-history))}}}

   (merge (util/common-resource :me/cards))))

(defn reload [store payment-gateway mimi]
  (-> {:methods
       {:post {:parameters {:query {:access_token String}
                            :path {:card-id String}
                            :body (-> schema :reload :post)}
               :response (fn [ctx]
                           (->
                            (d/let-flow [profile-data (p/get-deferred-profile store)
                                         card-data (p/get-deferred-card store (-> ctx :parameters :body :card-id))
                                         payment-method-data (p/get-deferred-payment-method-detail
                                                              store (-> ctx :parameters :body :paymentMethodId))
                                         payment-data (p/execute-payment payment-gateway
                                                                         {:firstName (-> profile-data :user :firstName)
                                                                          :lastName (-> profile-data :user :lastName)
                                                                          :emailAddress (-> profile-data :user :email)
                                                                          :routingNumber (-> payment-method-data :routingNumber)
                                                                          :cvn (-> payment-method-data :cvn)
                                                                          :transactionId "12345"
                                                                          :currency (-> card-data :balanceCurrencyCode)
                                                                          :amount (-> ctx :parameters :body :amount)})

                                         mimi-card-data (p/load-card mimi (-> ctx :parameters :body :card-id)
                                                                     (-> ctx :parameters :body :amount))]
                                        (util/>200 ctx {:cardId nil
                                                        :balance 416.02
                                                        :balanceDate "2014-03-03T20:17:51.4329837Z"
                                                        :balanceCurrencyCode "ZAR"
                                                        :cardNumber "7777064158671182"}))
                            (d/catch clojure.lang.ExceptionInfo
                                (fn [exception-info]
                                  (domain-exception ctx (ex-data exception-info))))))}}}

      (merge (util/common-resource :me/cards))))

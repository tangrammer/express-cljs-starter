(ns rebujito.api.resources.card
  (:require
   [taoensso.timbre :as log]
   [manifold.deferred :as d]
   [rebujito.api.resources :refer (domain-exception)]
   [rebujito.protocols :as p]
   [rebujito.api.util :as util]
   [rebujito.mongo :as mongo]
   [cheshire.core :as json]
   [schema.core :as s]
   [yada.resource :refer [resource]]))

(defn get-next-card-number []
  ; "9623570900021"
  "9623570900022")

(defn get-cards [store]
  (->
   {:methods
    {:get {:parameters {:query {:access_token String}}
           :consumes [{:media-type #{"application/json"}
                       :charset "UTF-8"}]
           :response (fn [ctx]
                                        ; TODO
                                        ;  (>200 ctx [(p/get-cards store)])
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

(def schema {:register-physical {:post {:cardNumber String
                                        :pin String}}
             :reload {:post {:amount Long
                             :paymentMethodId String
                             (s/optional-key :acceptTerms) Boolean
                             (s/optional-key :expirationYear) Long
                             (s/optional-key :expirationMonth) Long
                             (s/optional-key :sessionId) String}}})

(defn register-physical [store mimi user-store]
  (->
   {:methods
    {:post {:parameters {:query {:access_token String}
                         :body (-> schema :register-physical :post)}
            :response (fn [ctx]
                        (let [card-number #_(str (+ (rand-int 1000) (read-string (format "96235709%05d" 0))))
                              (get-in ctx [:parameters :body :cardNumber])]
                          (-> (d/let-flow [mimi-res (p/register-physical-card mimi {:cardNumber card-number
                                                                                    :customerId (-> (p/find user-store) last (get "_id") str mongo/id>mimi-id)})
                                           card (p/get-deferred-card store {})]
                                          (util/>200 ctx (assoc card :cardNumber card-number)))
                              (d/catch clojure.lang.ExceptionInfo
                                  (fn [exception-info]
                                    (domain-exception ctx (ex-data  exception-info))))))
                        )}}}

   (merge (util/common-resource :me/cards))))

(defn register-digital-card [store mimi user-store]
  (->
   {:methods
    {:post {:parameters {:query {:access_token String}}
            :response (fn [ctx]
                        (let [cardNumber (get-next-card-number)]
                          (-> (d/let-flow [mimi-res (p/register-physical-card mimi {:cardNumber cardNumber
                                                                                    :customerId  (-> (p/find user-store) last (get "_id") str mongo/id>mimi-id)})
                                           card (p/get-deferred-card store {})]

                                          (util/>200 ctx (assoc card :cardNumber cardNumber)))

                              (d/catch clojure.lang.ExceptionInfo
                                  (fn [exception-info]
                                    (domain-exception ctx (ex-data  exception-info))))
                              ))
                        )}}}

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
                                                                          :amount (-> ctx :parameters :body :amount)
                                                                          })
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

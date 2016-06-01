(ns rebujito.api.resources.payment
  (:refer-clojure :exclude [methods])
  (:require
   [taoensso.timbre :as log]
   [rebujito.protocols :as p]
   [rebujito.api.util :refer :all]
   [cheshire.core :as json]
   [schema.core :as s]
   [yada.resource :refer [resource]]))

(defn method-detail [store payment-gateway]
  (resource
   (->
    {:methods
     {:get {:parameters {:query {:access_token String (s/optional-key :select) String (s/optional-key :ignore) String}
                         :path {:payment-method-id String}}
            :consumes [{:media-type #{"application/json"}
                        :charset "UTF-8"}]

            :response (fn [ctx]
                        (let [paymentMethod (p/get-payment-method-detail store {:paymentMethodId (get-in ctx [:parameters :path :payment-method-id])})]
                          (println "paymentMethod" paymentMethod)
                          (if paymentMethod
                            (>200 ctx paymentMethod)
                            (>404 ctx ["Not Found"]))))}

      :delete {:parameters {:query {:access_token String}
                            :path {:payment-method-id String}}
               :consumes [{:media-type #{"application/json"}
                           :charset "UTF-8"}]

               :response (fn [ctx]
                           (let [paymentMethod (p/get-payment-method-detail store {:paymentMethodId (get-in ctx [:parameters :path :payment-method-id])})]
                             (println "paymentMethod" paymentMethod)
                             (if paymentMethod
                               (if (p/delete-card-token payment-gateway {:cardToken (-> paymentMethod :routingNumber)})
                                 (>200 ctx ["OK" "Success"])
                                 (>500 ctx ["Internal Server Error" "An unexpected error occurred processing the request."]))
                               (>404 ctx ["Not Found"]))))}

      :put {:parameters {:query {:access_token String}
                         :path {:payment-method-id String}
                         :body {:expirationYear Long
                                :billingAddressId String
                                :accountNumber String
                                :default Boolean
                                :nickname String
                                :paymentType String
                                :cvn String
                                :fullName String
                                :expirationMonth Long}}

            :consumes [{:media-type #{"application/json"}
                        :charset "UTF-8"}]

            :response (fn [ctx]
                        ; get existing payment method
                        (let [paymentMethod (p/get-payment-method-detail store {:paymentMethodId (get-in ctx [:parameters :path :payment-method-id])})]
                          (println "paymentMethod" paymentMethod)
                          (if paymentMethod
                            ; create a new token
                            (let [request (get-in ctx [:parameters :body])
                                  {:keys [cardToken]} (p/create-card-token payment-gateway
                                                                           {:cardNumber (-> request :accountNumber)
                                                                            :expirationMonth (-> request :expirationMonth)
                                                                            :expirationYear (-> request :expirationYear)
                                                                            :cvn (-> request :cvn)})]
                              (println "CardToken" cardToken)
                              (if cardToken
                                ; update payment method with new details and token
                                (let [updatedPaymentMethod (p/put-payment-method-detail store
                                                                              {:paymentMethodId (get-in ctx [:parameters :path :payment-method-id])
                                                                               :nickName (-> ctx :nickName)
                                                                               :paymentType (-> ctx :paymentType)
                                                                               :fullName (-> ctx :fullName)
                                                                               :default (-> ctx :default)
                                                                               :accountNumber "****************"
                                                                               :accountNumberLastFour "****"
                                                                               :cvn (-> ctx :cvn)
                                                                               :expirationMonth (-> ctx :expirationMonth)
                                                                               :expirationYear (-> ctx :expirationYear)
                                                                               :billingAddressId (-> ctx :billingAddressId)
                                                                               :routingNumber cardToken
                                                                               })]
                                  (println "updatedPaymentMethod" updatedPaymentMethod)
                                  (if updatedPaymentMethod
                                    (fn []
                                      ; delete the old token
                                      (try
                                        (p/delete-card-token payment-gateway {:cardToken (-> paymentMethod :routingNumber)})
                                        (catch Exception e
                                          ; ignore errors
                                          (println e)))
                                      ; return result
                                      (>200 ctx {
                                                 :fullName (-> updatedPaymentMethod :fullName)
                                                 :billingAddressId (-> updatedPaymentMethod :billingAddressId)
                                                 :accountNumber (-> updatedPaymentMethod :accountNumber)
                                                 :default (-> updatedPaymentMethod :default)
                                                 :paymentMethodId (-> updatedPaymentMethod :paymentMethodId)
                                                 :nickname (-> updatedPaymentMethod :nickname)
                                                 :paymentType (-> updatedPaymentMethod :paymentType)
                                                 :accountNumberLastFour (-> updatedPaymentMethod :accountNumberLastFour)
                                                 :cvn (-> updatedPaymentMethod :cvn)
                                                 :expirationYear (-> updatedPaymentMethod :expirationYear)
                                                 :expirationMonth (-> updatedPaymentMethod :expirationMonth)
                                                 :isTemporary (-> updatedPaymentMethod :isTemporary)
                                                 :bankName (-> updatedPaymentMethod :bankName)
                                                 :routingNumber (-> updatedPaymentMethod :routingNumber)
                                                 }))
                                    (>500 ctx ["An unexpected error occurred processing the request."])))))
                            (>404 ctx ["Not Found"]))))}}}

    (merge (common-resource "me/payment-methods/{payment-method-id}"))
    (merge access-control))))

(def schema {:methods {:post {:expirationYear Long
                              :billingAddressId String
                              :accountNumber String
                              :default String
                              :nickname String
                              :paymentType String
                              :cvn String
                              :fullName String
                              :expirationMonth Long}}})

(defn methods [store payment-gateway]
                                    (resource
                                      (-> {:methods
                                           {:get {:parameters {:query {:access_token String (s/optional-key :select) String (s/optional-key :ignore) String}}
                                                  :consumes [{:media-type #{"application/json"}
                                                              :charset "UTF-8"}]
                                                  :response (fn [ctx]
                                                              (let [paymentMethods (p/get-payment-method store)]
                                                                (println "paymentMethods" paymentMethods)
                                                                (if paymentMethods
                                                                  (>200 ctx paymentMethods)
                                                                  (>500 ctx ["An unexpected error occurred processing the request."]))))}

                                            :post {:parameters {:query {:access_token String}
                                                                :body (-> schema :methods :post)}
                                                   :consumes [{:media-type #{"application/json"}
                                                               :charset "UTF-8"}]
                                                   :response (fn [ctx]
                                                               (let [request (get-in ctx [:parameters :body])
                                                                     ; Create the token at the gateway
                                                                     {:keys [cardToken]} (p/create-card-token payment-gateway
                                                                                                              {:cardNumber (-> request :accountNumber)
                                                                                                               :expirationMonth (-> request :expirationMonth)
                                                                                                               :expirationYear (-> request :expirationYear)
                                                                                                               :cvn (-> request :cvn)})]
                                                                 (println "CardToken" cardToken)
                                                                 (if cardToken
                                                                   ; Create a new payment method with the Token
                                                                   (let [newPaymentMethod (p/post-payment-method store
                                                                                                                 {:nickName (-> request :nickName)
                                                                                                                  :paymentType (-> request :paymentType)
                                                                                                                  :fullName (-> request :fullName)
                                                                                                                  :default (-> request :default)
                                                                                                                  :accountNumber "****************"
                                                                                                                  :accountNumberLastFour "****"
                                                                                                                  :cvn (-> request :cvn)
                                                                                                                  :expirationMonth (-> request :expirationMonth)
                                                                                                                  :expirationYear (-> request :expirationYear)
                                                                                                                  :billingAddressId (-> request :billingAddressId)
                                                                                                                  :routingNumber cardToken
                                                                                                                  })]
                                                                     (println "newPaymentMethod" newPaymentMethod)
                                                                     (if newPaymentMethod
                                                                       (>201 ctx {
                                                                                  :fullName (-> newPaymentMethod :fullName)
                                                                                  :billingAddressId (-> newPaymentMethod :billingAddressId)
                                                                                  :accountNumber (-> newPaymentMethod :accountNumber)
                                                                                  :default (-> newPaymentMethod :default)
                                                                                  :paymentMethodId (-> newPaymentMethod :paymentMethodId)
                                                                                  :nickname (-> newPaymentMethod :nickname)
                                                                                  :paymentType (-> newPaymentMethod :paymentType)
                                                                                  :accountNumberLastFour (-> newPaymentMethod :accountNumberLastFour)
                                                                                  :cvn (-> newPaymentMethod :cvn)
                                                                                  :expirationYear (-> newPaymentMethod :expirationYear)
                                                                                  :expirationMonth (-> newPaymentMethod :expirationMonth)
                                                                                  :isTemporary (-> newPaymentMethod :isTemporary)
                                                                                  :bankName (-> newPaymentMethod :bankName)
                                                                                  :routingNumber (-> newPaymentMethod :routingNumber)
                                                                                  })
                                                                       (>500 ctx ["An unexpected error occurred processing the request."]))))))}}}
                                          (merge (common-resource :me/payment-methods))
                                          (merge access-control))))

(defn reload [store payment-gateway mimi]
  (resource
    (-> {:methods
         {:post {:parameters {:query {:access_token String}
                              :path {:card-id String}
                              :body {:amount Long
                                     :paymentMethodId String
                                     :sessionId String
                                     }}
                 :consumes [{:media-type #{"application/json"}
                             :charset "UTF-8"}]
                 :response (fn [ctx]
                             ;get the user
                             (let [profileData (p/get-profile store)]
                               (if profileData
                                 ; get the card
                                 (let [cardData (p/get-card store {:cardNumber (-> ctx :parameters :body :card-id)})]
                                   (if cardData
                                     ; get the payment method
                                     (let [paymentMethodData (p/get-payment-method-detail store (-> ctx :parameters :body :paymentMethodId))]
                                       (if paymentMethodData
                                         ; make the payment
                                         (let [paymentData (p/execute-payment payment-gateway {:firstName (-> profileData :user :firstName)
                                                                                               :lastName (-> profileData :user :lastName)
                                                                                               :emailAddress (-> profileData :user :email)
                                                                                               :routingNumber (-> paymentMethodData :routingNumber)
                                                                                               :cvn (-> paymentMethodData :cvn)
                                                                                               :transactionId "12345"
                                                                                               :currency (-> cardData :balanceCurrencyCode)
                                                                                               :amount (-> ctx :parameters :body :amount)
                                                                                               })]
                                           (if paymentData
                                             ; load card with credit via mimi
                                             (let [mimiData (p/load-card mimi {:cardId (-> cardData :cardNumber)
                                                                               :amount (-> paymentData :amount)})]
                                               (if mimiData
                                                 ; return balance
                                                 (>200 ctx {:cardId nil
                                                            :balance 416.02
                                                            :balanceDate "2014-03-03T20:17:51.4329837Z"
                                                            :balanceCurrencyCode "ZAR"
                                                            :cardNumber "7777064158671182"
                                                            })
                                                 (>500 ctx ["An unexpected error occurred debiting the card."])))
                                             (>500 ctx ["An unexpected error occurred processing the payment."])))
                                         (>404 ctx ["Payment Method Not Found"])))
                                     (>404 ctx ["Card Not Found"])))
                                 (>404 ctx ["Profile Not Found"]))))}}}
        
        (merge (common-resource :me/cards))
        (merge access-control))))
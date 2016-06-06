(ns rebujito.api.resources.payment
  (:refer-clojure :exclude [methods])
  (:require
   [manifold.deferred :as d]
   [taoensso.timbre :as log]
   [rebujito.protocols :as p]
   [rebujito.api.util :as util]
   [rebujito.api.resources :refer (domain-exception)]
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
                            (util/>200 ctx paymentMethod)
                            (util/>404 ctx ["Not Found"]))))}

      :delete {:parameters {:query {:access_token String}
                            :path {:payment-method-id String}}
               :consumes [{:media-type #{"application/json"}
                           :charset "UTF-8"}]

               :response (fn [ctx]
                           (let [paymentMethod (p/get-payment-method-detail store {:paymentMethodId (get-in ctx [:parameters :path :payment-method-id])})]
                             (println "paymentMethod" paymentMethod)
                             (if paymentMethod
                               (if (p/delete-card-token payment-gateway {:cardToken (-> paymentMethod :routingNumber)})
                                 (util/>200 ctx ["OK" "Success"])
                                 (util/>500 ctx ["Internal Server Error" "An unexpected error occurred processing the request."]))
                               (util/>404 ctx ["Not Found"]))))}

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
                                      (util/>200 ctx {
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
                                    (util/>500 ctx ["An unexpected error occurred processing the request."])))))
                            (util/>404 ctx ["Not Found"]))))}}}

    (merge (util/common-resource "me/payment-methods/{payment-method-id}"))
    (merge util/access-control))))

(def schema {:methods {:post {:expirationYear Long
                              :billingAddressId String
                              :accountNumber String
                              (s/optional-key :default) String
                              (s/optional-key :isDefault) Boolean
                              (s/optional-key :isTemporary) Boolean
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
                                                                  (util/>200 ctx paymentMethods)
                                                                  (util/>500 ctx ["An unexpected error occurred processing the request."]))))}

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
                                                                       (util/>201 ctx {
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
                                                                       (util/>500 ctx ["An unexpected error occurred processing the request."]))))))}}}
                                          (merge (util/common-resource :me/payment-methods))
                                          (merge util/access-control))))

(ns rebujito.api.resources
  (:require
   [rebujito.protocols :as p]
   [rebujito.api.util :refer :all]
   [cheshire.core :as json]
   [schema.core :as s]
   [yada.resource :refer [resource]]))

(def access-control
  {:access-control {}
   })

(defn fake-call [store]
  (resource
   (->
    {:methods
     {:get {:parameters {:path {:id Long}}
            :consumes [{:media-type #{"application/json" "application/xml"}
                        :charset "UTF-8"}]
            :response (fn [ctx]
                        {:id (get-in ctx [:parameters :path :id])
                         :message-for-mom "hi mom"
                         :camelCase :sux})}}}
    (merge (common-resource :fake-calls))
    (merge access-control))))

(defn register-digital-card [store]
  (resource
   (->
    {:methods
     {:post {:parameters {:query {:access_token String}}
             :consumes [{:media-type #{"application/json" "application/xml"}
                         :charset "UTF-8"}]

             :response (fn [ctx]
                         (condp = (get-in ctx [:parameters :query :access_token])
                           "400" (>400 ctx ["No registration address on file. Registration address must already exist for user."])
                           "500" (>500 ctx ["Internal Server Error :( "])
                           (>201 ctx (p/get-card store))
                           ))}}}
    (merge (common-resource :register-digital-card))
    (merge access-control))))

(defn payment-method-detail [store]
  (resource
   (->
    {:methods
     {:get {:parameters {:path {:payment-mehod-id String}}
            :consumes [{:media-type #{"application/json" "application/xml"}
                        :charset "UTF-8"}]

            :response (fn [ctx]
                        (condp = (get-in ctx [:parameters :path :payment-mehod-id])
                          "404" (>400 ctx ["Resource was not found"])
                          "500" (>500 ctx ["An unexpected error occurred processing the request."])
                          (>201 ctx (p/get-payment-method-detail store))
                          ))}}}
    (merge (common-resource :payment-methods-detail))
    (merge access-control))))

(defn payment-methods [store]
  (resource
   (-> {:methods
        {:get {:parameters {:query {:access_token String (s/optional-key :select) String (s/optional-key :ignore) String}}
               :consumes [{:media-type #{"application/json" "application/xml"}
                           :charset "UTF-8"}]
               :response (fn [ctx]
                           (condp = (get-in ctx [:parameters :query :access_token])
                             "500" (>500 ctx ["An unexpected error occurred processing the request."])
                             (>200 ctx (p/get-payment-method store))

                             ))}
         :post {:parameters {:query {:access_token String}
                             :body {:expirationYear Long
                                    :billingAddressId String
                                    :accountNumber String
                                    :default String
                                    :nickname String
                                    :paymentType String
                                    :cvn String
                                    :fullName String
                                    :expirationMonth Long}}
                :consumes [{:media-type #{"application/json" "application/xml"}
                            :charset "UTF-8"}]
                :response (fn [ctx]
                            (condp = (get-in ctx [:parameters :query :access_token])
                              "141000" (>400 ctx ["No Request Supplied" "Request was malformed."])
                              "141001" (>400 ctx ["PaymentType cannot be null or empty."
                                                  "Missing or Invalid type attribute"])
                              "141002" (>400 ctx ["FullName cannot be null or empty."
                                                  "Missing or Invalid fullName attribute."])
                              "141003" (>400 ctx ["AccountNumber cannot be null or empty."
                                                  "Missing or Invalid accountNumber attribute."])
                              "141004" (>400 ctx ["Cvn cannot be null or empty."
                                                  "Missing or Invalid accountCVN attribute."])
                              "141005" (>400 ctx ["Invalid ExpirationMonth."
                                                  "Invalid expirationMonth attribute."])
                              "141006" (>400 ctx ["Invalid ExpirationYear."
                                                  "Invalid expirationYear attribute."])
                              "141007" (>400 ctx ["AddressId cannot be null or empty."
                                                  "Missing or Invalid billingAddressId attribute."])
                              "141008" (>400 ctx ["Invalid PaymentMethod"	"Missing payment method object"])
                              "141025" (>400 ctx ["User cannot be null or empty"])
                              "141039" (>400 ctx ["Payment method already exists."])
                              "500" (>500 ctx ["An unexpected error occurred processing the request."])
                              (>201 ctx (p/post-payment-method store))

                              ))}}}
       (merge (common-resource :payment-methods))
       (merge access-control))))

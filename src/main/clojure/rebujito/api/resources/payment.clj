(ns rebujito.api.resources.payment
  (:refer-clojure :exclude [methods])
  (:require
   [manifold.deferred :as d]
   [taoensso.timbre :as log]
   [rebujito.protocols :as p]
   [rebujito.api.util :as util]
   [rebujito.scopes :as scopes]
   [rebujito.api.resources :refer (domain-exception)]
   [cheshire.core :as json]
   [schema.core :as s]
   [yada.resource :refer [resource]]))

(def schema {:methods {:post {
                              :accountNumber String
                              :nickname String
                              :paymentType String
                              :cvn String
                              (s/optional-key :billingAddressId) String
                              (s/optional-key :default) String
                              (s/optional-key :isDefault) Boolean
                              (s/optional-key :isTemporary) Boolean
                              (s/optional-key :fullName) String
;                              (s/optional-key :risk) s/Any
                              :expirationMonth Long
                              :expirationYear Long}}
             :method-detail {:put {:expirationYear Long
                                   :billingAddressId String
                                   :accountNumber String
                                   :default Boolean
                                   :nickname String
                                   :paymentType String
                                   :cvn String
                                   :fullName String
                                   :expirationMonth Long}}})

(defn method-detail [store payment-gateway]
  (->
   {:methods
    {:get {:parameters {:query {:access_token String (s/optional-key :select) String (s/optional-key :ignore) String}
                        :path {:payment-method-id String}}
           :response (fn [ctx]
                       (-> (d/let-flow [payment-method (p/get-deferred-payment-method-detail store {:paymentMethodId (get-in ctx [:parameters :path :payment-method-id])})]
                                       (util/>200 ctx payment-method))
                           (d/catch clojure.lang.ExceptionInfo
                               (fn [exception-info]
                                 (domain-exception ctx (ex-data exception-info))))))}

     :delete {:parameters {:query {:access_token String}
                           :path {:payment-method-id String}}
              :response (fn [ctx]
                          (-> (d/let-flow [payment-method (p/get-deferred-payment-method-detail store {:paymentMethodId (get-in ctx [:parameters :path :payment-method-id])})
                                        res-delete (p/delete-card-token payment-gateway {:cardToken (-> payment-method :routingNumber)})]
                                       (util/>200 ctx ["OK" "Success"]))
                              (d/catch clojure.lang.ExceptionInfo
                                 (fn [exception-info]
                                   (domain-exception ctx (ex-data exception-info))))))}

     :put {:parameters {:query {:access_token String}
                        :path {:payment-method-id String}
                        :body (-> schema :method-detail :put)}
           :response (fn [ctx]
                       (let [request (get-in ctx [:parameters :body])]
                         (-> (d/let-flow [payment-method (p/get-deferred-payment-method-detail store {:paymentMethodId (get-in ctx [:parameters :path :payment-method-id])})
                                          card-token-res @(p/create-card-token payment-gateway
                                                                               {:cardNumber (-> request :accountNumber)
                                                                                :expirationMonth (-> request :expirationMonth)
                                                                                :expirationYear (-> request :expirationYear)
                                                                                :cvn (-> request :cvn)})
                                          card-token (:card-token card-token-res)
                                          updated-payment-method (p/put-payment-method-detail store
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
                                                                                               :routingNumber card-token
                                                                                               })
                                          res-delete (p/delete-card-token payment-gateway {:cardToken (-> payment-method :routingNumber)})]
                                         (util/>200 ctx {
                                                         :fullName (-> updated-payment-method :fullName)
                                                         :billingAddressId (-> updated-payment-method :billingAddressId)
                                                         :accountNumber (-> updated-payment-method :accountNumber)
                                                         :default (-> updated-payment-method :default)
                                                         :paymentMethodId (-> updated-payment-method :paymentMethodId)
                                                         :nickname (-> updated-payment-method :nickname)
                                                         :paymentType (-> updated-payment-method :paymentType)
                                                         :accountNumberLastFour (-> updated-payment-method :accountNumberLastFour)
                                                         :cvn (-> updated-payment-method :cvn)
                                                         :expirationYear (-> updated-payment-method :expirationYear)
                                                         :expirationMonth (-> updated-payment-method :expirationMonth)
                                                         :isTemporary (-> updated-payment-method :isTemporary)
                                                         :bankName (-> updated-payment-method :bankName)
                                                         :routingNumber (-> updated-payment-method :routingNumber)
                                                         })
                                         )
                             (d/catch clojure.lang.ExceptionInfo
                                 (fn [exception-info]
                                   (domain-exception ctx (ex-data exception-info)))))))}}}

   (merge (util/common-resource "me/payment-methods/{payment-method-id}"))))

(defn- take-last* [s n]
  (apply str (take-last n s)))



(defn adapt-mongo-to-spec [payment-method]
  (-> payment-method
      (assoc :accountNumber (:routingNumber payment-method)
             :nickname (:nickName payment-method)
             :type (:paymentType payment-method))
      (dissoc :paymentType :nickName :routingNumber)))

(defn methods [user-store payment-gateway]
  (-> {:methods
       {:get {:parameters {:query {:access_token String (s/optional-key :select) String (s/optional-key :ignore) String}}
              :response (fn [ctx]
                          (-> (d/let-flow [auth-user (util/authenticated-user ctx)
                                           payment-methods (->> (or (:paymentMethods (p/find user-store (:_id auth-user)))
                                                                   [])
                                                                (map adapt-mongo-to-spec))]
                                          (log/info  "paymentMethods GET" payment-methods)
                                          (util/>200 ctx payment-methods))
                              (d/catch clojure.lang.ExceptionInfo
                                  (fn [exception-info]
                                    (domain-exception ctx (ex-data exception-info))))))}

        :post {:parameters {:query {:access_token String}
                            :body (-> schema :methods :post)}
               :response (fn [ctx]
                           (-> (d/let-flow [request (get-in ctx [:parameters :body])
                                            auth-user (util/authenticated-user ctx)
                                            card-token (p/create-card-token payment-gateway
                                                                            {:cardNumber (-> request :accountNumber)
                                                                             :expirationMonth (-> request :expirationMonth)
                                                                             :expirationYear (-> request :expirationYear)
                                                                             :cvn (-> request :cvn)})

                                            new-payment-method (p/add-new-payment-method
                                                                user-store
                                                                (:_id auth-user)
                                                                (merge {
                                                                        :accountNumberLastFour (take-last* (-> request :accountNumber) 4)

                                                                        :expirationMonth (-> request :expirationMonth)
                                                                        :expirationYear (-> request :expirationYear)
                                                                        :nickName (-> request :nickName)
                                                                        :paymentType (-> request :paymentType)
                                                                        :routingNumber (:card-token card-token)
                                                                        }
                                                                       (util/field? request :billingAddressId)
                                                                       (util/field? request :default)
                                                                       (util/field? request :fullName)
                                                                       ))]
                                           (util/>200 ctx {
                                                           :accountNumber nil
                                                           :accountNumberLastFour (take-last* (-> request :accountNumber) 4)
                                                           :bankName nil
                                                           :billingAddressId (-> request :billingAddressId)
                                                           :cvn nil
                                                           :default (-> request :default)
                                                           :expirationMonth (-> request :expirationMonth)
                                                           :expirationYear (-> request :expirationYear)
                                                           :fullName (-> request :fullName)

                                                           :isTemporary true ;;TODO: ;(-> new-payment-method :isTemporary)
                                                           :nickname (-> request :nickname)
                                                           :paymentMethodId (-> new-payment-method :paymentMethodId)
                                                           :paymentType (-> request :paymentType)
                                                           :routingNumber nil
                                                           }))
                               (d/catch clojure.lang.ExceptionInfo
                                   (fn [exception-info]
                                     (domain-exception ctx (ex-data exception-info))))))}}}
      (merge (util/common-resource :me/payment-methods))))

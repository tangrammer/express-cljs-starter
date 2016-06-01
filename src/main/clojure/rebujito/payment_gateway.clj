(ns rebujito.payment-gateway
  (:require
   [schema.core :as s]
   [taoensso.timbre :as log]
   [rebujito.protocols :as protocols]
   [com.stuartsierra.component  :as component]
   [org.httpkit.client :as http-k]
   [ring.velocity.core :as velocity]
   [clj-xpath.core :as xp]))

(defrecord Paygate []
  component/Lifecycle
  (start [this]
    this)
  (stop [this] this)
  protocols/PaymentGateway
  (ping [this data]
    @(http-k/post (-> this :url)
                  {:body (velocity/render "paygate/ping.vm"
                                          :paygateId (-> this :paygateId)
                                          :paygatePassword (-> this :paygatePassword)
                                          :identifier "test")}
                  (fn [{:keys [status body error]}]
                    (log/debug status error body)
                    (if (or error (not= 200 status) (not (.contains body "PingResponse")) (.contains body "SOAP-ENV:Fault|payhost:error"))
                      (throw (ex-info "500" "Paygate Not Available"))
                      ))))
  (create-card-token [this data]
    (log/debug data)
    @(http-k/post (-> this :url)
                 {:body (velocity/render "paygate/create-card-token.vm"
                                         :paygateId (-> this :paygateId)
                                         :paygatePassword (-> this :paygatePassword)
                                         :cardNumber (-> data :cardNumber)
                                         :expirationMonth (-> data :expirationMonth)
                                         :expirationYear (-> data :expirationYear))}
                  (fn [{:keys [status body error]}]
                    (log/debug status error body)
                    (if (or error (not= 200 status) (not (.contains body "CardVaultResponse")) (.contains body "SOAP-ENV:Fault|payhost:error"))
                      nil
                      (let [response (xp/xml->doc body)
                            cardToken (xp/$x:text* "/Envelope/Body/SingleVaultResponse/CardVaultResponse/Status/VaultId" response)]
                        (if (and (not-empty cardToken) (first cardToken))
                          {:cardToken (first cardToken)}
                          nil))))))
  (delete-card-token [this data]
    (log/debug data)
    @(http-k/post (-> this :url)
                  {:body (velocity/render "paygate/delete-card-token.vm"
                                          :paygateId (-> this :paygateId)
                                          :paygatePassword (-> this :paygatePassword)
                                          :vaultId (-> data :cardToken))}
                  (fn [{:keys [status body error]}]
                    (println "Delete Card Token Response" status error body)
                    (if (or error (not= 200 status) (not (.contains body "DeleteVaultResponse")) (.contains body "SOAP-ENV:Fault|payhost:error"))
                      false
                      (let [response (xp/xml->doc body)
                            result (xp/$x:text* "/Envelope/Body/SingleVaultResponse/DeleteVaultResponse/Status/StatusName" response)]
                        (and (not-empty result) (= (first result)  "Completed"))
                        )))))
  (execute-payment [this data]
    (log/debug data)
    @(http-k/post (-> this :url)
                  {:body (velocity/render "paygate/payment-with-token.vm"
                                          :paygateId (-> this :paygateId)
                                          :paygatePassword (-> this :paygatePassword)
                                          :firstName (-> data :firstName)
                                          :lastName (-> data :lastName)
                                          :emailAddress (-> data :emailAddress)
                                          :cardToken (-> data :accountNumber)
                                          :cvn (-> data :cvn)
                                          :transactionId (-> data :transactionId)
                                          :currency "ZAR"
                                          :amount (-> data :amount))
                   }
                  (fn [{:keys [status body error]}]
                    (log/debug status error body)
                    (if (or error (not= 200 status) (not (.contains body "CardPaymentResponse")) (.contains body "SOAP-ENV:Fault|payhost:error"))
                      false
                      (let [response (xp/xml->doc body)
                            TransactionId (xp/$x:text* "/Envelope/Body/SinglePaymentResponse/CardPaymentResponse/Status/TransactionId" response)
                            StatusName (xp/$x:text* "/Envelope/Body/SinglePaymentResponse/CardPaymentResponse/Status/StatusName" response)
                            AuthCode (xp/$x:text* "/Envelope/Body/SinglePaymentResponse/CardPaymentResponse/Status/AuthCode" response)
                            PayRequestId (xp/$x:text* "/Envelope/Body/SinglePaymentResponse/CardPaymentResponse/Status/PayRequestId" response)
                            TransactionStatusCode (xp/$x:text* "/Envelope/Body/SinglePaymentResponse/CardPaymentResponse/Status/TransactionStatusCode" response)
                            TransactionStatusDescription (xp/$x:text* "/Envelope/Body/SinglePaymentResponse/CardPaymentResponse/Status/TransactionStatusDescription" response)
                            ResultCode (xp/$x:text* "/Envelope/Body/SinglePaymentResponse/CardPaymentResponse/Status/ResultCode" response)
                            ResultDescription (xp/$x:text* "/Envelope/Body/SinglePaymentResponse/CardPaymentResponse/Status/ResultDescription" response)
                            ]
                        (log/debug "TransactionId" TransactionId 
                                   "StatusName" StatusName 
                                   "AuthCode" AuthCode
                                   "PayRequestId" PayRequestId
                                   "TransactionStatusCode" TransactionStatusCode
                                   "TransactionStatusDescription" TransactionStatusDescription
                                   "ResultCode" ResultCode
                                   "ResultDescription" ResultDescription
                                   )
                        (and (not-empty TransactionStatusCode) (= (first TransactionStatusCode)  "1"))
                        ))))))

(defrecord MockPaymentGateway []
  component/Lifecycle
  (start [this]
    (println "-----Paygate-STUB----")
    this)
  (stop [this] this)
  protocols/PaymentGateway
  (ping [this data])
  (create-card-token [this data]
    {:status "200"
     :cardToken "123abc"})
  (delete-card-token [this data] {:status "completed"})
  (execute-payment [this data]
    {:status "completed"
     :transactionId "23423554252"
     :reference "33256w456345"
     :payRequestId "0B282A21-865C-484C-9A21-D9211F8CCEA2"
     :transactionStatusCode "1"
     :resultCode "Approved"
     }))

(def PaygateConfigSchema
  {:paygateId s/Str
   :paygatePassword s/Str
   :url s/Str})

(defn new-prod-payment-gateway [opts]
  (s/validate PaygateConfigSchema opts)
  (map->Paygate opts))

(defn new-mock-payment-gateway [opts]
  (map->MockPaymentGateway opts))

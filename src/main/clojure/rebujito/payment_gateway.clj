(ns rebujito.payment-gateway
  (:require
   [cheshire.core :as json]
   [manifold.deferred :as d]
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
  ;; (ping [this data]
  ;;   @(http-k/post (-> this :url)
  ;;                 {:body (velocity/render "paygate/ping.vm"
  ;;                                         :paygateId (-> this :paygateId)
  ;;                                         :paygatePassword (-> this :paygatePassword)
  ;;                                         :identifier "test")}
  ;;                 (fn [{:keys [status body error]}]
  ;;                   (log/info "PayGate Ping Response" status error body)
  ;;                   (if (or error (not= 200 status) (not (.contains body "PingResponse")) (.contains body "SOAP-ENV:Fault|payhost:error"))
  ;;                     (throw (ex-info "500" "Paygate Not Available"))
  ;;                     ))))
  (create-card-token [this data]
    (let [data (update data :expirationMonth #(format "%02d" %))
          _     (s/validate {:cardNumber String
                 :expirationYear Long
                 :expirationMonth String} data)
          d* (d/deferred)]
      (log/info "PayGate Create Card Token Request" data)
      (http-k/post (-> this :url)
                    {:body (velocity/render "paygate/create-card-token.vm"
                                            :paygateId (-> this :paygateId)
                                            :paygatePassword (-> this :paygatePassword)
                                            :cardNumber (-> data :cardNumber)
                                            :expirationMonth (-> data :expirationMonth)
                                            :expirationYear (-> data :expirationYear))}
                    (fn [{:keys [status body error]}]
                      (log/info "PayGate Create Card Token Response" status error body)
                      (if (or error (not= 200 status) (not (.contains body "CardVaultResponse")) (.contains body "SOAP-ENV:Fault|payhost:error"))
                        (d/error! d* (ex-info (str "error!!!" 500)
                                              {:type :payment-gateway
                                               :status 500
                                               :body (json/generate-string ["paygate/create-card-token" status error body])}))
                        (let [response (xp/xml->doc body)
                              card-token (xp/$x:text* "/Envelope/Body/SingleVaultResponse/CardVaultResponse/Status/VaultId" response)]
                          (if (first card-token)
                            (d/success! d* {:card-token (first card-token)})
                            (d/error! d* (ex-info (str "error!!!" 500)
                                                  {:type :payment-gateway
                                                   :status 500
                                                   :body (json/generate-string ["paygate/create-card-token NULL card-token" status error body])})))))))
      d*))
  (delete-card-token [this data]
    (let [d* (d/deferred)]
      (log/info "PayGate Delete Card Token Request" data)
      (http-k/post (-> this :url)
                   {:body (velocity/render "paygate/delete-card-token.vm"
                                            :paygateId (-> this :paygateId)
                                            :paygatePassword (-> this :paygatePassword)
                                            :vaultId (-> data :cardToken))}
                    (fn [{:keys [status body error]}]
                      (log/info "PayGate Delete Card Token Response" status error body)
                      (if (or error (not= 200 status) (not (.contains body "DeleteVaultResponse")) (.contains body "SOAP-ENV:Fault|payhost:error"))
                        (d/error! d* (ex-info (str "error!!!" 500)
                                              {:type :payment-gateway
                                               :status 500
                                               :body (json/generate-string ["paygate/delete-card-token" status error body])}))
                        (let [response (xp/xml->doc body)
                              result (xp/$x:text* "/Envelope/Body/SingleVaultResponse/DeleteVaultResponse/Status/StatusName" response)]
                          (if (and (not-empty result) (= (first result)  "Completed"))
                            (d/success! d* true)
                            (d/error! d* (ex-info (str "error!!!" 500)
                                                  {:type :payment-gateway
                                                   :status 500
                                                   :body (json/generate-string ["paygate/delete-card-token" status error body])})))))))
      d*))
  (execute-payment [this data]

    (s/validate {:firstName String
                 :lastName String
                 :emailAddress String
                 :routingNumber String
                 :cvn String
                 :transactionId String
;                 :currency "ZAR"
                 :amount Long
                 } data)
    (let [d* (d/deferred)]
      (log/debug "PayGate Payment Request" data)
      (http-k/post (-> this :url)
                   {:body (velocity/render "paygate/payment-with-token.vm"
                                           :paygateId (-> this :paygateId)
                                           :paygatePassword (-> this :paygatePassword)
                                           :firstName (-> data :firstName)
                                           :lastName (-> data :lastName)
                                           :emailAddress (-> data :emailAddress)
                                           :cardToken (-> data :routingNumber)
                                           :cvn (-> data :cvn)
                                           :transactionId (-> data :transactionId)
                                           :currency "ZAR"
                                           :amount (-> data :amount))
                    }
                   (fn [{:keys [status body error]}]
                     (log/info "PayGate Payment Response" status error body)
                     (if (or error (not= 200 status) (not (.contains body "CardPaymentResponse")) (.contains body "SOAP-ENV:Fault|payhost:error"))
                       (d/error! d* (ex-info (str "API GATEWAY ERROR!")
                                             {:type :payment-gateway
                                              :status 500
                                              :body (str "An unexpected error occurred processing the payment."
                                                         error "  ::  " status)}))

                       (let [response (xp/xml->doc body)
                             TransactionId (xp/$x:text* "/Envelope/Body/SinglePaymentResponse/CardPaymentResponse/Status/TransactionId" response)
                             StatusName (xp/$x:text* "/Envelope/Body/SinglePaymentResponse/CardPaymentResponse/Status/StatusName" response)
                             AuthCode (xp/$x:text* "/Envelope/Body/SinglePaymentResponse/CardPaymentResponse/Status/AuthCode" response)
                             PayRequestId (xp/$x:text* "/Envelope/Body/SinglePaymentResponse/CardPaymentResponse/Status/PayRequestId" response)
                             TransactionStatusCode (xp/$x:text* "/Envelope/Body/SinglePaymentResponse/CardPaymentResponse/Status/TransactionStatusCode" response)
                             TransactionStatusDescription (xp/$x:text* "/Envelope/Body/SinglePaymentResponse/CardPaymentResponse/Status/TransactionStatusDescription" response)
                             ResultCode (xp/$x:text* "/Envelope/Body/SinglePaymentResponse/CardPaymentResponse/Status/ResultCode" response)
                             ResultDescription (xp/$x:text* "/Envelope/Body/SinglePaymentResponse/CardPaymentResponse/Status/ResultDescription" response)
                             res-debug (vector "TransactionId" TransactionId
                                               "StatusName" StatusName
                                               "AuthCode" AuthCode
                                               "PayRequestId" PayRequestId
                                               "TransactionStatusCode" TransactionStatusCode
                                               "TransactionStatusDescription" TransactionStatusDescription
                                               "ResultCode" ResultCode
                                               "ResultDescription" ResultDescription
                                               )]
                         (log/info res-debug)
                         (if (and (not-empty TransactionStatusCode) (= (first TransactionStatusCode)  "1"))
                           (d/success! d* res-debug)
                           (d/error! d* (ex-info (str "Payment Failed!")
                                                 {:type :payment-gateway
                                                  :status 500
                                                  :body (json/generate-string ["paygate/payment" res-debug])})))
                         ))))
      d*)))

(defrecord MockPaymentGateway []
  component/Lifecycle
  (start [this]
    (log/info "-----Paygate-STUB----")
    this)
  (stop [this] this)
  protocols/PaymentGateway
;;  (ping [this data])
  (create-card-token [this data]
    (let [d* (d/deferred)]
      (d/success! d* {:status "200"
                   :cardToken "123abc"})
      d*
      ))
  (delete-card-token [this data] {:status "completed"})
  (execute-payment [this data]
    (let [d* (d/deferred)]
      (d/success! d* {:status "completed"
                      :transactionId "23423554252"
                      :reference "33256w456345"
                      :payRequestId "0B282A21-865C-484C-9A21-D9211F8CCEA2"
                      :transactionStatusCode "1"
                      :resultCode "Approved"
                      })
      d*)))

(def PaygateConfigSchema
  {:paygateId s/Str
   :paygatePassword s/Str
   :url s/Str})

(defn new-prod-payment-gateway [opts]
  (s/validate PaygateConfigSchema opts)
  (map->Paygate opts))

(defn new-mock-payment-gateway [opts]
  (map->MockPaymentGateway opts))

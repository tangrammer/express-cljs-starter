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
    ["200"
     "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\">
   <SOAP-ENV:Header/>
   <SOAP-ENV:Body>
      <ns2:PingResponse xmlns:ns2=\"http://www.paygate.co.za/PayHOST\"/>
   </SOAP-ENV:Body>
</SOAP-ENV:Envelope>"])
  (create-card-token [this data]
    (log/debug data)
    @(http-k/post (-> this :url)
                 {:body (velocity/render "paygate/vault.vm"
                                         :paygateId (-> this :paygateId)
                                         :paygatePassword (-> this :paygatePassword)
                                         :cardNumber "4000000000000002"
                                         :expirationMonth 11
                                         :expirationYear 2018)}
                  (fn [{:keys [status body error]}]
                    (log/debug status error body)
                    (if (or error (not= 200 status)) 
                      {:status "500"}
                      (let [response (xp/xml->doc body)]
                        {
                         :expirationYear 0
                         :billingAddressId nil
                         :accountNumber "F67A77FC92D518AC9BF69B1028BCF7A711B1"
                         :default false
                         :paymentMethodId (xp/$x:text "/Envelope/Body/SingleVaultResponse/CardVaultResponse/Status/VaultId" response)
                         :nickname "nick"
                         :paymentType "visa"
                         :accountNumberLastFour nil
                         :cvn nil
                         :fullName "fullname"
                         :expirationMonth 0
                         :isTemporary false
                         :bankName nil
                         }
                        )))))
  (delete-card-token [this data]
    ["200"
     "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\">
   <SOAP-ENV:Header/>
   <SOAP-ENV:Body>
      <ns2:SingleVaultResponse xmlns:ns2=\"http://www.paygate.co.za/PayHOST\">
         <ns2:DeleteVaultResponse>
            <ns2:Status>
               <ns2:StatusName>Completed</ns2:StatusName>
               <ns2:StatusDetail>Vault successfull</ns2:StatusDetail>
            </ns2:Status>
         </ns2:DeleteVaultResponse>
      </ns2:SingleVaultResponse>
   </SOAP-ENV:Body>
</SOAP-ENV:Envelope>"])
  (execute-payment [this data]
    ["200"
     "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\">
   <SOAP-ENV:Header/>
   <SOAP-ENV:Body>
      <ns2:SinglePaymentResponse xmlns:ns2=\"http://www.paygate.co.za/PayHOST\">
         <ns2:CardPaymentResponse>
            <ns2:Status>
               <ns2:TransactionId>38430056</ns2:TransactionId>
               <ns2:Reference>123456789</ns2:Reference>
               <ns2:AcquirerCode>00</ns2:AcquirerCode>
               <ns2:StatusName>Completed</ns2:StatusName>
               <ns2:AuthCode>J3BBBZ</ns2:AuthCode>
               <ns2:PayRequestId>0B282A21-865C-484C-9A21-D9211F8CCEA2</ns2:PayRequestId>
               <ns2:TransactionStatusCode>1</ns2:TransactionStatusCode>
               <ns2:TransactionStatusDescription>Approved</ns2:TransactionStatusDescription>
               <ns2:ResultCode>990017</ns2:ResultCode>
               <ns2:ResultDescription>Auth Done</ns2:ResultDescription>
               <ns2:Currency>ZAR</ns2:Currency>
               <ns2:Amount>100</ns2:Amount>
               <ns2:RiskIndicator>XX</ns2:RiskIndicator>
               <ns2:PaymentType>
                  <ns2:Method>CC</ns2:Method>
                  <ns2:Detail>Visa</ns2:Detail>
               </ns2:PaymentType>
            </ns2:Status>
         </ns2:CardPaymentResponse>
      </ns2:SinglePaymentResponse>
   </SOAP-ENV:Body>
</SOAP-ENV:Envelope>"]))


(defrecord MockPaymentGateway []
  component/Lifecycle
  (start [this]
    (println "-----Paygate-STUB----")
    this)
  (stop [this] this)
  protocols/PaymentGateway
  (ping [this data]
    ["200"
     "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\">
   <SOAP-ENV:Header/>
   <SOAP-ENV:Body>
      <ns2:PingResponse xmlns:ns2=\"http://www.paygate.co.za/PayHOST\"/>
   </SOAP-ENV:Body>
</SOAP-ENV:Envelope>"])
  (create-card-token [this data]
    ["200"
     "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\">
   <SOAP-ENV:Header/>
   <SOAP-ENV:Body>
      <ns2:SingleVaultResponse xmlns:ns2=\"http://www.paygate.co.za/PayHOST\">
         <ns2:CardVaultResponse>
            <ns2:Status>
               <ns2:StatusName>Completed</ns2:StatusName>
               <ns2:StatusDetail>Vault successfull</ns2:StatusDetail>
               <ns2:VaultId>c57173a4-f313-465a-be1a-28030d38d5c0</ns2:VaultId>
            </ns2:Status>
         </ns2:CardVaultResponse>
      </ns2:SingleVaultResponse>
   </SOAP-ENV:Body>
</SOAP-ENV:Envelope>"])
  (delete-card-token [this data]
    ["200"
     "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\">
   <SOAP-ENV:Header/>
   <SOAP-ENV:Body>
      <ns2:SingleVaultResponse xmlns:ns2=\"http://www.paygate.co.za/PayHOST\">
         <ns2:DeleteVaultResponse>
            <ns2:Status>
               <ns2:StatusName>Completed</ns2:StatusName>
               <ns2:StatusDetail>Vault successfull</ns2:StatusDetail>
            </ns2:Status>
         </ns2:DeleteVaultResponse>
      </ns2:SingleVaultResponse>
   </SOAP-ENV:Body>
</SOAP-ENV:Envelope>"])
  (execute-payment [this data]
    ["200"
     "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\">
   <SOAP-ENV:Header/>
   <SOAP-ENV:Body>
      <ns2:SinglePaymentResponse xmlns:ns2=\"http://www.paygate.co.za/PayHOST\">
         <ns2:CardPaymentResponse>
            <ns2:Status>
               <ns2:TransactionId>38430056</ns2:TransactionId>
               <ns2:Reference>123456789</ns2:Reference>
               <ns2:AcquirerCode>00</ns2:AcquirerCode>
               <ns2:StatusName>Completed</ns2:StatusName>
               <ns2:AuthCode>J3BBBZ</ns2:AuthCode>
               <ns2:PayRequestId>0B282A21-865C-484C-9A21-D9211F8CCEA2</ns2:PayRequestId>
               <ns2:TransactionStatusCode>1</ns2:TransactionStatusCode>
               <ns2:TransactionStatusDescription>Approved</ns2:TransactionStatusDescription>
               <ns2:ResultCode>990017</ns2:ResultCode>
               <ns2:ResultDescription>Auth Done</ns2:ResultDescription>
               <ns2:Currency>ZAR</ns2:Currency>
               <ns2:Amount>100</ns2:Amount>
               <ns2:RiskIndicator>XX</ns2:RiskIndicator>
               <ns2:PaymentType>
                  <ns2:Method>CC</ns2:Method>
                  <ns2:Detail>Visa</ns2:Detail>
               </ns2:PaymentType>
            </ns2:Status>
         </ns2:CardPaymentResponse>
      </ns2:SinglePaymentResponse>
   </SOAP-ENV:Body>
</SOAP-ENV:Envelope>"]))

(def PaygateConfigSchema
  {:paygateId s/Str
   :paygatePassword s/Str
   :url s/Str})

(defn new-prod-payment-gateway [opts]
  (s/validate PaygateConfigSchema opts)
  (map->Paygate opts))

(defn new-mock-payment-gateway [opts]
  (map->MockPaymentGateway opts))
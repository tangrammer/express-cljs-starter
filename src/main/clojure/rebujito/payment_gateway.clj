(ns rebujito.payment-gateway
  (:require
   [rebujito.protocols :as protocols]
   [com.stuartsierra.component  :as component]))


(defrecord Paygate [paygate-account]
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


(defrecord MockPaymentGateway []
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

(defn new-prod-payment-gateway [payment-gateway-config]
  (map->Paygate payment-gateway-config))

(defn new-mock-payment-gateway [payment-gateway-config]
  (map->MockPaymentGateway payment-gateway-config))

(ns rebujito.payment-gateway-test
  (:require
   [org.httpkit.client :as http-k]
   [byte-streams :as bs]
   [cheshire.core :as json]
   [clojure.pprint :refer (pprint)]
   [clojure.test :refer :all]
   [rebujito.config :refer (config)]
   [ring.velocity.core :as velocity]
   ))


(deftest velocityRenderingTest
  (is (= "hello,dennis,your age is 29." (velocity/render "test.vm" :name "dennis" :age 29)))
  (let [{:keys [status body]}
        (-> @(http-k/post "https://secure.paygate.co.za/payhost/process.trans"
                          {:body (velocity/render "paygate/ping.vm"
                                                  :paygateId (config [:payment-gateway :paygate-account :paygateId])
                                                  :paygatePassword (config [:payment-gateway :paygate-account :paygatePassword])
                                                  :identifier "test")}))]
    (is (= 200 status))
    (is (.contains body "PingResponse"))
    (is (not (.contains body "SOAP-ENV:Fault|payhost:error")))))

(deftest paygateVaultTest
  (let [{:keys [status body]}
        (-> @(http-k/post "https://secure.paygate.co.za/payhost/process.trans"
                          {:body (velocity/render "paygate/create-card-token.vm"
                                                  :paygateId (config [:payment-gateway :paygate-account :paygateId])
                                                  :paygatePassword (config [:payment-gateway :paygate-account :paygatePassword])
                                                  :cardNumber "4000000000000002"
                                                  :expirationMonth 11
                                                  :expirationYear 2018
                                                  )}))]
    (is (= 200 status))
    (is (.contains body "CardVaultResponse"))
    (is (not (.contains body "SOAP-ENV:Fault|payhost:error")))))

(deftest paygatePaymentTest
  (let [{:keys [status body]}
        (-> @(http-k/post "https://secure.paygate.co.za/payhost/process.trans"
                          {:body (velocity/render "paygate/payment-with-token.vm"
                                                  :paygateId (config [:payment-gateway :paygate-account :paygateId])
                                                  :paygatePassword (config [:payment-gateway :paygate-account :paygatePassword])
                                                  :firstName "Stefan"
                                                  :lastName "Tester"
                                                  :emailAddress "stefan.tester@example.com"
                                                  :addresslines ["1 Main Street" "Building"]
                                                  :cardToken "7a17ba06-deab-4e53-b7d9-ca4cfc3d94ec"
                                                  :cvn "123"
                                                  :transactionId "12345"
                                                  :currency "ZAR"
                                                  :amount 100
                                                  )}))
        ]
    (is (= 200 status))
    (is (.contains body "CardPaymentResponse"))
    (is (not (.contains body "SOAP-ENV:Fault|payhost:error")))))

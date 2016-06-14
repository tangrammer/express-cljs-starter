(ns rebujito.payment-gateway-test
  (:require
   [clojure.data.xml :as xml]
   [org.httpkit.client :as http-k]
   [byte-streams :as bs]
   [rebujito.base-test :refer (*system* system-fixture)]
   [cheshire.core :as json]
   [clojure.pprint :refer (pprint)]
   [clojure.test :refer :all]
   [rebujito.config :refer (config)]
   [rebujito.protocols :as p]
   [manifold.deferred :as d]
   [ring.velocity.core :as velocity]
   ))


(use-fixtures :each (system-fixture #{:+mock-mimi :+ephemeral-db}))

(defn printxml [body] (let [input-xml (java.io.StringReader. body)]
         (pprint (xml/parse input-xml))))

(deftest velocityRenderingTest
  (is (= "hello,dennis,your age is 29." (velocity/render "test.vm" :name "dennis" :age 29)))
  (let [{:keys [status body]}
        (-> @(http-k/post "https://secure.paygate.co.za/payhost/process.trans"
                          {:body (velocity/render "paygate/ping.vm"
                                                  :paygateId (get-in (config :test) [:payment-gateway :paygate :paygateId])
                                                  :paygatePassword (get-in (config :test) [:payment-gateway :paygate :paygatePassword])
                                                  :identifier "test")}))]
    (is (= 200 status))
    (is (.contains body "PingResponse"))
    (is (not (.contains body "SOAP-ENV:Fault|payhost:error")))))

(deftest paygateVaultTest
  (testing "create-token-card"
    (is (let [r (:card-token @(p/create-card-token (:payment-gateway *system*) {:cardNumber "4000000000000002"
                                                                               :expirationYear 2018
                                                                                :expirationMonth 11}))]
          (println "card-token: " r)
          r))

    (is (let [r (:card-token @(p/create-card-token (:payment-gateway *system*) {:cardNumber "4000000000000002"
                                                                               :expirationYear 2018
                                                                                :expirationMonth 6}))]
          (println "card-token: " r)
          r))





    )
)

(deftest paygatePaymentTest
  (let [{:keys [status body]}
        (-> @(http-k/post "https://secure.paygate.co.za/payhost/process.trans"
                          {:body (velocity/render "paygate/payment-with-token.vm"
                                                  :paygateId (get-in (config :test) [:payment-gateway :paygate :paygateId])
                                                  :paygatePassword (get-in (config :test) [:payment-gateway :paygate :paygatePassword])
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
    (clojure.pprint/pprint body)
    (is (.contains body "CardPaymentResponse"))
    (is (not (.contains body "SOAP-ENV:Fault|payhost:error")))))

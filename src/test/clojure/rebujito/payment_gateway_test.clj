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


(use-fixtures :each (system-fixture #{:+mock-mimi :+ephemeral-db :+mock-mailer}))

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

(deftest paygateVaultTest ;; create card token
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


    (let [r (-> (p/create-card-token (:payment-gateway *system*) {:cardNumber "2222222222222222"
                                                                  :expirationYear 2018
                                                                  :expirationMonth 6})
                (d/catch clojure.lang.ExceptionInfo
                    (fn [exception-info]
                      (:status (ex-data exception-info))
                     ))
                )]
      (is (= 400 @r))
      r)))

(deftest paygatePaymentTest ;; execute payment

  (testing "execute-payment"
    (let [token (atom "")]
     (is (let [r (:card-token @(p/create-card-token (:payment-gateway *system*) {:cardNumber "4000000000000002"
                                                                                 :expirationYear 2018
                                                                                 :expirationMonth 6}))]
           (println "card-token: " r)
           (reset! token r)
           r))

     (is (let [r  @(p/execute-payment (:payment-gateway *system*)
                                                 {:firstName "Stefan"
                                                  :lastName "Tester"
                                                  :emailAddress "stefan.tester@example.com"
                                        ;                                                    :addresslines ["1 Main Street" "Building"]
                                                  :routingNumber @token
                                                  :cvn "123"
                                                  :transactionId "12345"
                                                  :currency "ZAR"
                                                  :amount 100
                                                  })]
           (println "execute-payment: " r)
           r)))



    )

)

(ns rebujito.api-test
  (:require
   [aleph.http :as http]
   [org.httpkit.client :as http-k]
   [bidi.bidi :as bidi]
   [byte-streams :as bs]
   [cheshire.core :as json]
   [clojure.pprint :refer (pprint)]
   [clojure.test :refer :all]
   [com.stuartsierra.component :as component]
   [rebujito.api.resources.account :as account]
   [rebujito.api.resources.card :as card]
   [rebujito.api.resources.oauth :as oauth]
   [rebujito.api.resources.payment :as payment]
   [rebujito.api.resources.social-profile :as social-profile]
   [rebujito.config :refer (config)]
   [rebujito.system :refer (new-production-system)]
   [schema-generators.generators :as g]
   [schema.core :as s]
   [ring.velocity.core :as x]))


(def ^:dynamic *system* nil)

(defmacro with-system [system & body]
  `(let [s# (component/start ~system)]
     (try
       (binding [*system* s#] ~@body)
       (finally
         (component/stop s#)))))

(defn system-fixture [f]
  (with-system (-> (new-production-system (update-in (config :test) [:yada :port] inc)))
    (try
      (s/with-fn-validation
        (f))
      (catch Exception e (do (println (str "caught exception: " (.getMessage e)))
                             (throw e))))))


(use-fixtures :each system-fixture)


(deftest test-20*
  (time
   (let [r (-> *system* :docsite-router :routes)
         port (-> *system*  :webserver :port)]
     (testing ::account/create
       (let [api-id ::account/create
             path (bidi/path-for r api-id)]
         (is (= 201 (-> @(http/post (format "http://localhost:%s%s?access_token=%s&market=%s"  port path 123 1234)
                                    {:throw-exceptions false
                                     :body (json/generate-string
                                            (g/generate (:post account/schema)))
                                     :body-encoding "UTF-8"
                                     :content-type :json})
                        :status)))))

     (testing ::oauth/token-resource-owner
       (let [api-id ::oauth/token-resource-owner
             path (bidi/path-for r api-id)]

         ;; body conform :token-refresh-token schema
         (is (= 200 (-> @(http/post (format "http://localhost:%s%s?sig="  port path 123)
                                    {:throw-exceptions false
                                     :body (json/generate-string
                                            (g/generate (-> oauth/schema :token-refresh-token :post)))
                                     :body-encoding "UTF-8"
                                     :content-type :json})
                        :status)))
         ;; body conform :token-resource-owner schema
         (is (= 200 (-> @(http/post (format "http://localhost:%s%s?sig="  port path 123)
                                    {:throw-exceptions false
                                     :body (json/generate-string
                                            (g/generate (-> oauth/schema :token-resource-owner :post)))
                                     :body-encoding "UTF-8"
                                     :content-type :json})
                        :status)))))



     (testing ::card/get-cards
       (let [api-id ::card/get-cards
             path (bidi/path-for r api-id)]
         (is (= 201 (-> @(http/get (format "http://localhost:%s%s?access_token=%s"  port path 123)
                                   {:throw-exceptions false
                                    :body-encoding "UTF-8"
                                    :content-type :json})
                        :status)))))

     (testing ::card/register-physical
       (let [api-id ::card/register-physical
             path (bidi/path-for r api-id)]
         (println (format "http://localhost:%s%s?access_token=%s"  port path 123))
         (is (= 200 (-> @(http/post (format "http://localhost:%s%s?access_token=%s"  port path 123)
                                    {:throw-exceptions false
                                     :body-encoding "UTF-8"
                                     :body (json/generate-string
                                            (g/generate (-> card/schema :post :register-physical)))
                                     :content-type :json})
                        :status)))))

     (testing ::card/register-digital-cards
       (let [api-id ::card/register-digital-cards
             path (bidi/path-for r api-id)]
         (println (format "http://localhost:%s%s?access_token=%s"  port path 123))
         (is (= 201 (-> @(http/post (format "http://localhost:%s%s?access_token=%s"  port path 123)
                                    {:throw-exceptions false
                                     :body-encoding "UTF-8"
                                     :content-type :json})
                        :status)))))

     (testing ::card/unregister
       (let [api-id ::card/unregister
             path (bidi/path-for r api-id :card-id 123)]
         (is (= 200 (-> @(http/delete (format "http://localhost:%s%s?access_token=%s"  port path 123)
                                      {:throw-exceptions false
                                       :body-encoding "UTF-8"
                                       :content-type :json})
                        :status)))))

     (testing ::payment/methods
       (let [api-id ::payment/methods
             path (bidi/path-for r api-id)]
         (is (= 200 (-> @(http/get (format "http://localhost:%s%s?access_token=%s"  port path 123)
                                   {:throw-exceptions false
                                    :body-encoding "UTF-8"
                                    :content-type :json})
                        :status)))
         (is (= 201 (-> @(http/post (format "http://localhost:%s%s?access_token=%s"  port path 123)
                                   {:throw-exceptions false
                                    :body-encoding "UTF-8"
                                    :body (json/generate-string
                                           (g/generate (-> payment/schema :methods :post)))
                                    :content-type :json})
                        :status)))))

     (testing ::social-profile/account
       (let [api-id ::social-profile/account
             path (bidi/path-for r api-id)]
         (is (= 200 (-> @(http/put (format "http://localhost:%s%s?access_token=%s"  port path 123)
                                   {:throw-exceptions false
                                    :body-encoding "UTF-8"
                                    :body (json/generate-string
                                           (g/generate (-> social-profile/schema :put)))
                                    :content-type :json})
                        :status)))))))





  (comment
    "here the way to parse the byte-stream body response"

    (let [{:keys [status body]}
          (-> @(http-k/get "https://api.swarmloyalty.co.za/" )


              )]
      status
      )

    (-> @(http/get "https://api.swarmloyalty.co.za" {:insecure? true})
        :body
        )))

(deftest aTest
  (is (= 4 (+ 2 2)))
  (is (= 7 (+ 3 4))))

(deftest velocityRenderingTest
  (is (= "hello,dennis,your age is 29." (x/render "test.vm" :name "dennis" :age 29)))
  (let [{:keys [status body]}
        (-> @(http-k/post "https://secure.paygate.co.za/payhost/process.trans"
                          {:body (x/render "paygate/ping.vm"
                                           :paygateId (config [:paygate-account :paygateId])
                                           :paygatePassword (config [:paygate-account :paygatePassword]) 
                                           :identifier "test")}))]
    (is (= 200 status))
    (is (.contains body "PingResponse"))
    (is (not (.contains body "SOAP-ENV:Fault|payhost:error")))))

(deftest paygateVaultTest
  (let [{:keys [status body]}
        (-> @(http-k/post "https://secure.paygate.co.za/payhost/process.trans"
                          {:body (x/render "paygate/vault.vm"
                                           :paygateId (config [:paygate-account :paygateId])
                                           :paygatePassword (config [:paygate-account :paygatePassword])
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
                          {:body (x/render "paygate/payment.vm"
                                           :paygateId (config [:paygate-account :paygateId])
                                           :paygatePassword (config [:paygate-account :paygatePassword])
                                           :firstName "Stefan" 
                                           :lastName "Tester" 
                                           :emailAddress "stefan.tester@example.com"
                                           :addresslines ["1 Main Street" "Building"]
                                           :cardToken "7a17ba06-deab-4e53-b7d9-ca4cfc3d94ec"
                                           :cvn "123"
                                           :transactionId "12345"
                                           :currency "ZAR"
                                           :amount 100
                                           )}))]
    (is (= 200 status))
    (is (.contains body "CardPaymentResponse"))
    (is (not (.contains body "SOAP-ENV:Fault|payhost:error")))))

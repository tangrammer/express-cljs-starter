(ns rebujito.api.resources.payment-test
  (:require
   [byte-streams :as bs]
   [bidi.bidi :as bidi]
   [rebujito.config :refer (config)]
   [schema-generators.generators :as g]
   [cheshire.core :as json]
   [manifold.deferred :as d]
   [rebujito.protocols :as p]
   [rebujito.api-test :refer (system-fixture *system* get-path access-token-application
                                             access-token-user new-account-sb create-account new-sig print-body api-config)]
   [aleph.http :as http]
   [rebujito.api.resources
    [payment :as payment]
    [account :as account]
    [oauth :as oauth]
    [login :as login]]
   [clojure.test :refer :all]))


(use-fixtures :each (system-fixture #{:+mock-mimi :+ephemeral-db}))

(deftest payment-resource
  (testing ::payment/methods
    (let [port (-> *system*  :webserver :port)
          account-data  (assoc (new-account-sb)
                               :birthDay "1"
                               :birthMonth "1")
          account (create-account  account-data)

          access_token (access-token-user (:emailAddress account-data)(:password account-data))
          ]
      (let [path (get-path ::payment/methods)]
       ;;         (println (format "http://localhost:%s%s?access_token=%s"  port path 123))
       (is (= 200 (-> @(http/get (format "http://localhost:%s%s?access_token=%s"  port path access_token)
                                 {:throw-exceptions false
                                  :body-encoding "UTF-8"
                                  :content-type :json})
                      :status)))
       (let [{:keys [status body]}
             (-> @(http/post (format "http://localhost:%s%s?access_token=%s"  port path access_token)
                             {:throw-exceptions false
                              :body-encoding "UTF-8"
                              :body (json/generate-string
                                     {
                                      :expirationYear 2018
                                      :billingAddressId "string"
                                      :accountNumber "4000000000000002"
                                      :default "false"
                                      :nickname "string"
                                      :paymentType "visa"
                                      :cvn "12345"
                                      :fullName "string"
                                      :expirationMonth 11
                                      }
                                     )
                              :content-type :json}))
             body (-> (bs/to-string body)
                      (json/parse-string true))
             ]
         (is (= status 201))

         (clojure.pprint/pprint body)
         (is  (:paymentMethodId body))
         ))))

  )

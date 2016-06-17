(ns rebujito.api.resources.address-test
  (:require
   [schema.core :as s]
   [schema-generators.generators :as g]
   [byte-streams :as bs]
   [bidi.bidi :as bidi]
   [rebujito.config :refer (config)]
   [rebujito.protocols :as p]
   [schema-generators.generators :as g]
   [cheshire.core :as json]
   [manifold.deferred :as d]
   [rebujito.protocols :as p]
   [rebujito.api-test :refer (print-body create-digital-card parse-body)]
   [rebujito.base-test :refer (system-fixture *system* *user-access-token* get-path  access-token-application access-token-user new-account-sb create-account new-sig  api-config)]
   [aleph.http :as http]
   [rebujito.api.resources
    [payment :as payment]
    [addresses :as addresses]
    [card :as card]
    [account :as account]
    [oauth :as oauth]
    [login :as login]]
   [clojure.test :refer :all]))


(use-fixtures :each (system-fixture #{:+mock-mimi :+ephemeral-db}))

(deftest address-testing
  (testing ::addresses/addresses
    (let [port (-> *system*  :webserver :port)
          address-id (atom "")]
      ;; get all
      (let [path (get-path ::addresses/addresses)
            http-response @(http/get (format "http://localhost:%s%s?access_token=%s"  port path *user-access-token*)
                                     {:throw-exceptions false
                                      :body-encoding "UTF-8"
                                      :content-type :json})
            body (parse-body http-response)
            ]
        (is (= 200 (-> http-response :status)))
        (is (= [] body)))

      ;; create!
      (let [path (get-path ::addresses/addresses)
            http-res @(http/post (format "http://localhost:%s%s?access_token=%s"  port path *user-access-token*)
                                 {:throw-exceptions false
                                  :body-encoding "UTF-8"
                                  :body (json/generate-string
                                         (g/generate (:post addresses/schema))

                                         )
                                  :content-type :json})
            _ (is (= (:status http-res) 201))
            body (parse-body http-res)]

        ;; checking the header "/me/addresses/a8963052-9131-475d-9a23-40a3d2f109cc"
        (is  (:location (clojure.walk/keywordize-keys (-> http-res :headers ))))
        (reset! address-id (last (clojure.string/split (:location (clojure.walk/keywordize-keys (-> http-res :headers ))) #"\/")))
        )

      ;; get-detail
      (let [path (bidi/path-for (-> *system* :docsite-router :routes) ::addresses/get :address-id @address-id)
            http-response @(http/get (format "http://localhost:%s%s?access_token=%s"  port path *user-access-token*)
                                     {:throw-exceptions false
                                      :body-encoding "UTF-8"
                                      :content-type :json})
            body (parse-body http-response)
            ]
        (is (= 200 (-> http-response :status)))
        (is (= '(:addressLine1 :city :addressId :firstName :type :addressLine2 :lastName :postalCode :phoneNumber :country) (keys body))))


(comment



 (let [path (get-path ::payment/methods)
       http-response @(http/get (format "http://localhost:%s%s?access_token=%s"  port path *user-access-token*)
                                {:throw-exceptions false
                                 :body-encoding "UTF-8"
                                 :content-type :json})
       body (parse-body http-response)
       ]
   (is (= 200 (-> http-response :status)))
   (try (s/validate {:expirationYear Long
                     :billingAddressId String
                     :default String
                     :paymentMethodId String
                     :type String
                     (s/optional-key :routingNumber) String
                     :accountNumberLastFour String
                     :nickname s/Any
                     :fullName String
                     (s/optional-key :accountNumber) String
                     :expirationMonth Long}
                    (payment/adapt-mongo-to-spec (first (:paymentMethods (p/find (:user-store *system*) (:_id (p/read-token  (-> *system* :authenticator) *user-access-token*)))))))

        (catch Exception e (is (nil? e)))))


 (let [path (bidi/path-for (-> *system* :docsite-router :routes) ::payment/method-detail :payment-method-id @payment-method-id)
       _ (println (format "http://localhost:%s%s?access_token=%s"  port path  *user-access-token*))
       http-response @(http/get (format "http://localhost:%s%s?access_token=%s"  port path  *user-access-token*)
                                {:throw-exceptions false
                                 :body-encoding "UTF-8"
                                 :content-type :json})
       body (parse-body http-response)
       ]
   (is (= 200 (-> http-response :status)))

   (try
     (s/validate  {:expirationYear s/Num
                   :billingAddressId (s/maybe String)
                   :accountNumber String
                   :default Boolean
                   :paymentMethodId String
                   :nickname (s/maybe String)
                   :paymentType String
                   :accountNumberLastFour (s/maybe String)
                   :cvn (s/maybe String)
                   :fullName String
                   :routingNumber (s/maybe String)
                   :expirationMonth s/Num
                   :isTemporary Boolean
                   :bankName (s/maybe String)}
                  body)
     (catch Exception e (is (nil? e)))))

 ;; delete
 (let [path (get-path ::payment/methods)
       {:keys [status body]}
       (-> @(http/post (format "http://localhost:%s%s?access_token=%s"  port path *user-access-token*)
                       {:throw-exceptions false
                        :body-encoding "UTF-8"
                        :body (json/generate-string
                               {
                                :billingAddressId "string"
                                :accountNumber "4000000000000003"
                                :default "false"
                                :nickname "string"
                                :paymentType "visa"
                                :cvn "12345"
                                :fullName "string"
                                :expirationMonth 11
                                :expirationYear 2018
                                }
                               )
                        :content-type :json}))
       body (-> (bs/to-string body)
                (json/parse-string true))]

   (is (= status 200))
   (is  (:paymentMethodId body))

   (let [path (bidi/path-for (-> *system* :docsite-router :routes) ::payment/method-detail :payment-method-id (:paymentMethodId body))
         http-response @(http/delete (format "http://localhost:%s%s?access_token=%s"  port path  *user-access-token*)
                                     {:throw-exceptions false
                                      :body-encoding "UTF-8"
                                      :content-type :json})
         body (parse-body http-response)
         ]

     (is (= 200 (-> http-response :status)))
     (is (= ["OK" "Success" true] body))

     )

   (let [path (bidi/path-for (-> *system* :docsite-router :routes) ::payment/method-detail :payment-method-id (:paymentMethodId body))
         http-response @(http/get (format "http://localhost:%s%s?access_token=%s"  port path  *user-access-token*)
                                  {:throw-exceptions false
                                   :body-encoding "UTF-8"
                                   :content-type :json})
         body (parse-body http-response)
         ]

     (is (= 400 (-> http-response :status)))


     )



   )

 ;; update
 (let [path (get-path ::payment/methods)
       {:keys [status body]}
       (-> @(http/post (format "http://localhost:%s%s?access_token=%s"  port path *user-access-token*)
                       {:throw-exceptions false
                        :body-encoding "UTF-8"
                        :body (json/generate-string
                               {
                                :billingAddressId "string"
                                :accountNumber "4000000000000004"
                                :default "false"
                                :nickname "string"
                                :paymentType "visa"
                                :cvn "12345"
                                :fullName "string"
                                :expirationMonth 11
                                :expirationYear 2018
                                }
                               )
                        :content-type :json}))
       body (-> (bs/to-string body)
                (json/parse-string true))]

   (is (= status 200))
   (is  (:paymentMethodId body))

   (let [path (bidi/path-for (-> *system* :docsite-router :routes) ::payment/method-detail :payment-method-id (:paymentMethodId body))
         http-response @(http/put (format "http://localhost:%s%s?access_token=%s"  port path  *user-access-token*)
                                  {:throw-exceptions false
                                   :body-encoding "UTF-8"
                                   :content-type :json
                                   :body (json/generate-string
                                          {
                                           :billingAddressId "string"
                                           :accountNumber "4000000000000004"
                                           :default "false"
                                           :nickname "string"
                                           :paymentType "visa"
                                           :cvn "12345"
                                           :fullName "string"
                                           :expirationMonth 11
                                           :expirationYear 2018
                                           })})
         body (parse-body http-response)
         ]

     (is (= 200 (-> http-response :status)))
     (is (= {:expirationYear 2018,
             :billingAddressId "string",
             :accountNumber "****************",
             :default false,
             :paymentMethodId (:paymentMethodId body),
             :nickname nil,
             :paymentType "visa",
             :accountNumberLastFour "****",
             :cvn "12345",
             :fullName "string",
             ;; avoid this generated value => :routingNumber "b0f20d9e-601a-4969-8230-354418cad5a8",
             :expirationMonth 11,
             :isTemporary nil,
             :bankName nil}
            (select-keys body [:paymentMethodId :expirationYear :billingAddressId :accountNumber :default :nickname :paymentType :accountNumberLastFour :cvn :fullName  :expirationMonth :isTemporary :bankName])))

     )

   (let [path (bidi/path-for (-> *system* :docsite-router :routes) ::payment/method-detail :payment-method-id (:paymentMethodId body))
         http-response @(http/get (format "http://localhost:%s%s?access_token=%s"  port path  *user-access-token*)
                                  {:throw-exceptions false
                                   :body-encoding "UTF-8"
                                   :content-type :json})
         body (parse-body http-response)
         ]

     (is (= 200 (-> http-response :status)))


     )



   ))



      )
    )




  )

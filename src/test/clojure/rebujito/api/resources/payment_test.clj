(ns rebujito.api.resources.payment-test
  (:require
   [taoensso.timbre :as log]
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
    [card :as card]
    [account :as account]
    [oauth :as oauth]
    [login :as login]]
   [clojure.test :refer :all]))


(use-fixtures :each (system-fixture #{:+mock-mimi :+ephemeral-db}))

(deftest payment-resource
  (testing ::payment/methods
    (let [port (-> *system*  :webserver :port)
          payment-method-id (atom "")
          card-id (create-digital-card)]
      (log/info "testing " :GET ::payment/methods)
      (let [path (get-path ::payment/methods)
            http-response @(http/get (format "http://localhost:%s%s?access_token=%s"  port path *user-access-token*)
                                     {:throw-exceptions false
                                      :body-encoding "UTF-8"
                                      :content-type :json})
            body (parse-body http-response)
            ]
        (is (= 200 (-> http-response :status)))
        (is (= [] body)))

      (log/info "testing " :POST ::payment/methods)
      (let [path (get-path ::payment/methods)
            {:keys [status body] :as all}
            (-> @(http/post (format "http://localhost:%s%s?access_token=%s"  port path *user-access-token*)
                            {:throw-exceptions false
                             :body-encoding "UTF-8"
                             :body (json/generate-string
                                    {
                                     :billingAddressId "string"
                                     :accountNumber "4000000000000002"
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
                    _ (is (= status 200))
            body (-> (bs/to-string body)
                     (json/parse-string true))]


        (is  (:paymentMethodId body))
        (reset! payment-method-id (:paymentMethodId body))
        )

      (log/info "testing" :GET ::payment/methods)
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

      (log/info "testing" :GET ::payment/method-detail)
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



        )


      ;;TODO: we have a difference between mongo schema and specification schema
      ;; https://admin.swarmloyalty.co.za/sbdocs/docs/starbucks_api/my_starbucks_profile/get_payment_methods.html
      (let [mi-res (set '(:expirationYear :billingAddressId :default :paymentMethodId :paymentType :accountNumberLastFour :nickName :fullName :routingNumber :expirationMonth))
            expected-res (set '(:expirationYear :billingAddressId :accountNumber :default :paymentMethodId :nickname :type :accountNumberLastFour :fullName :expirationMonth))
            ]
        [(clojure.set/difference mi-res expected-res) (clojure.set/difference  expected-res mi-res)])
           [#{:paymentType :nickName :routingNumber} #{:accountNumber :nickname :type}]

      ;; ::card/reload

      (let [api-id ::card/reload
            r (-> *system* :docsite-router :routes)
            path (bidi/path-for r api-id :card-id card-id)]
        ;;         (println (format "http://localhost:%s%s?access_token=%s"  port path card-id))
        (is (= 200(-> @(http/post (format "http://localhost:%s%s?access_token=%s"  port path *user-access-token*)
                                  {:throw-exceptions false
                                   :body-encoding "UTF-8"
                                   :body (json/generate-string
                                          {
                                           :amount 15
                                           :paymentMethodId @payment-method-id
                                           :sessionId ""
                                           })
                                   :content-type :json})
                                        ;                      (print-body)
                      :status))))


      (let [api-id ::card/autoreload
            r (-> *system* :docsite-router :routes)
            path (bidi/path-for r api-id :card-id card-id)]
        ;;         (println (format "http://localhost:%s%s?access_token=%s"  port path card-id))
        (is (= 200(-> @(http/post (format "http://localhost:%s%s?access_token=%s"  port path *user-access-token*)
                                  {:throw-exceptions false
                                   :body-encoding "UTF-8"
                                   :body (json/generate-string
                                          {
                                           :amount 15.00,
                                           :autoReloadType "Amount",
                                           :day 0,
                                           :paymentMethodId @payment-method-id
                                           :status "active",
                                           :triggerAmount 10.00,
                                           }
                                          )
                                   :content-type :json})
                      (print-body)
                      :status)))



        (let [ar (-> (p/find (-> *system* :user-store) (:_id (p/read-token (-> *system* :authenticator) *user-access-token*) ))
                     :cards first :autoReloadProfile
                     )]
          (is ar)
          (is (:active ar))
          )

        ;; validation amount with autoReloadType="Amount"
        (let [{:keys [status body]} @(http/post (format "http://localhost:%s%s?access_token=%s"  port path *user-access-token*)
                                                {:throw-exceptions false
                                                 :body-encoding "UTF-8"
                                                 :body (json/generate-string
                                                        {
                                                         :amount 0,
                                                         :autoReloadType "Amount",
                                                         :day 0,
                                                         :paymentMethodId @payment-method-id
                                                         :status "active",
                                                         :triggerAmount 10.00,
                                                         }
                                                        )
                                                 :content-type :json})]
          (is (= 400 status))
          (is (= (-> (bs/to-string body)
                     (json/parse-string true)) {:code 12345,
                                                :message
                                                "Please supply an auto reload amount. :: Missing or invalid auto reload amount attribute is required. Amount must be within the range of 10-1000",
                                                :body
                                                "Please supply an auto reload amount. :: Missing or invalid auto reload amount attribute is required. Amount must be within the range of 10-1000"})))

        ;; validation amount with autoReloadType="Date"
        (let [{:keys [status body]} @(http/post (format "http://localhost:%s%s?access_token=%s"  port path *user-access-token*)
                                                {:throw-exceptions false
                                                 :body-encoding "UTF-8"
                                                 :body (json/generate-string
                                                        {
                                                         :amount 0,
                                                         :autoReloadType "Date",
                                                         :day 0,
                                                         :paymentMethodId @payment-method-id
                                                         :status "active",
                                                         :triggerAmount 0
                                                         }
                                                        )
                                                 :content-type :json})]
          (is (= 400 status))
          (is (= (-> (bs/to-string body)
                     (json/parse-string true)) {:code 12345,
   :message
   "Please supply an auto reload type. :: Missing or invalid auto reload type attribute is required. Type must be set to either 'date' or 'amount'.",
   :body
   "Please supply an auto reload type. :: Missing or invalid auto reload type attribute is required. Type must be set to either 'date' or 'amount'."})))


        ;; validation payment-method
        (let [{:keys [status body]} @(http/post (format "http://localhost:%s%s?access_token=%s"  port path *user-access-token*)
                                                {:throw-exceptions false
                                                 :body-encoding "UTF-8"
                                                 :body (json/generate-string
                                                        {
                                                         :amount 0,
                                                         :autoReloadType "Amount",
                                                         :day 0,
                                        ;                                                    :paymentMethodId @payment-method-id
                                                         :status "active",
                                                         :triggerAmount 10.00,
                                                         }
                                                        )
                                                 :content-type :json})]
          (is (= 400 status))
          (is (= (-> (bs/to-string body)
                     (json/parse-string true)) {:code 12345,
   :message
   "Please supply a payment method id. :: Missing payment method identifier attribute is required",
   :body
   "Please supply a payment method id. :: Missing payment method identifier attribute is required"})))

        ;; validation reload-type
        (let [{:keys [status body]} @(http/post (format "http://localhost:%s%s?access_token=%s"  port path *user-access-token*)
                                                {:throw-exceptions false
                                                 :body-encoding "UTF-8"
                                                 :body (json/generate-string
                                                        {
                                                         :amount 0,
                                        ;                   :autoReloadType "Amount",
                                                         :day 0,
                                                         :paymentMethodId @payment-method-id
                                                         :status "active",
                                                         :triggerAmount 10.00,
                                                         }
                                                        )
                                                 :content-type :json})]
          (is (= 400 status))
          (is (= (-> (bs/to-string body)
                     (json/parse-string true)) {:code 12345,
   :message
   "Please supply an auto reload type. :: Missing or invalid auto reload type attribute is required. Type must be set to either 'date' or 'amount'.",
   :body
   "Please supply an auto reload type. :: Missing or invalid auto reload type attribute is required. Type must be set to either 'date' or 'amount'."})))
        ;; validation proper reload-type
        (let [{:keys [status body]} @(http/post (format "http://localhost:%s%s?access_token=%s"  port path *user-access-token*)
                                                {:throw-exceptions false
                                                 :body-encoding "UTF-8"
                                                 :body (json/generate-string
                                                        {
                                                         :amount 0,
                                                         :autoReloadType "xxx",
                                                         :day 0,
                                                         :paymentMethodId @payment-method-id
                                                         :status "active",
                                                         :triggerAmount 10.00,
                                                         }
                                                        )
                                                 :content-type :json})]
          (is (= 400 status))
          (is (= (-> (bs/to-string body)
                     (json/parse-string true)) {:code 12345,
   :message
   "Please supply an auto reload type. :: Missing or invalid auto reload type attribute is required. Type must be set to either 'date' or 'amount'.",
   :body
   "Please supply an auto reload type. :: Missing or invalid auto reload type attribute is required. Type must be set to either 'date' or 'amount'."})))

        )

      (let [api-id ::card/autoreload-disable
            r (-> *system* :docsite-router :routes)
            path (bidi/path-for r api-id :card-id card-id)]
        ;;         (println (format "http://localhost:%s%s?access_token=%s"  port path card-id))
        (is (= 200(-> @(http/put (format "http://localhost:%s%s?access_token=%s"  port path *user-access-token*)
                                 {:throw-exceptions false
                                  :body-encoding "UTF-8"
                                  :content-type :json})
                      (print-body)
                      :status)))



        (let [ar (-> (p/find (-> *system* :user-store) (:_id (p/read-token (-> *system* :authenticator) *user-access-token*) ))
                     :cards first :autoReloadProfile
                     )]
          (is (false? (:active ar)))))



      )
    )




  )

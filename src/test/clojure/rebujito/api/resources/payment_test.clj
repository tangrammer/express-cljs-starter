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
   [rebujito.api-test :refer (print-body create-digital-card* parse-body)]
   [rebujito.base-test :refer (system-fixture *system* *user-access-token* get-path  access-token-application access-token-user new-account-sb create-account new-sig  api-config)]
   [aleph.http :as http]
   [rebujito.api.resources
    [payment :as payment]
    [card :as card]
    [account :as account]
    [oauth :as oauth]
    [login :as login]]
   [rebujito.api.resources.card
    [reload :as card-reload]]
   [clojure.test :refer :all]))


(use-fixtures :each (system-fixture #{:+mock-mimi :+ephemeral-db :+mock-mailer}))

(deftest payment-resource
  (testing ::payment/methods
    (let [port (-> *system*  :webserver :port)
          r (-> *system* :docsite-router :routes)
          payment-method-id (atom "")
          card (create-digital-card*)
          card-id (:cardId card)
          ]
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
                          :paymentType String
                          (s/optional-key :routingNumber) String
                          :accountNumberLastFour String
                          :nickname s/Any
                          :fullName String
                          (s/optional-key :accountNumber) String
                          :expirationMonth Long}
                         (payment/adapt-mongo-to-spec (first (:paymentMethods (p/find (:user-store *system*) (:user-id (p/read-token  (-> *system* :authenticator) *user-access-token*)))))))

             (catch Exception e (is (nil? e)))))

      (log/info "testing" :GET ::payment/method-detail)
      (let [path (bidi/path-for r ::payment/method-detail :payment-method-id @payment-method-id)
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

        (let [path (bidi/path-for (-> *system* :docsite-router :routes) ::payment/delete-method-detail :payment-method-id (:paymentMethodId body))
              http-response @(http/post (format "http://localhost:%s%s?access_token=%s"  port path  *user-access-token*)
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


      (let [api-id ::card-reload/reload
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



        (let [ar (-> (p/find (-> *system* :user-store) (:user-id (p/read-token (-> *system* :authenticator) *user-access-token*) ))
                     :cards first :autoReloadProfile
                     )]
          (is (false? (:active ar)))))



      )
    ))


(deftest card-check-reload
  (testing ::card-reload/check

    (let [port (-> *system*  :webserver :port)
          r (-> *system* :docsite-router :routes)
          payment-method-id (atom "")
          card (create-digital-card*)
          card-id (:cardId card)
          ]

      (let [path (bidi/path-for r ::card-reload/check :card-number (:cardNumber card))
            res @(http/get (format "http://localhost:%s%s"  port path)
                           {:throw-exceptions false
                            :body-encoding "UTF-8"

                            :content-type :json})
            body (parse-body res)
            ]
                                        ;          (is (= nil body))
        (is (= 200 (-> res :status)))
        (is (= nil body))
        (let [mails @(:mails (:mailer *system*))]
          (is (= 1 (count mails)))))

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

      (let [path (bidi/path-for r ::card/autoreload :card-id card-id)]
        (is (= 200(-> @(http/post (format "http://localhost:%s%s?access_token=%s"  port path *user-access-token*)
                                  {:throw-exceptions false
                                   :body-encoding "UTF-8"
                                   :body (json/generate-string
                                          {
                                           :amount 149.00,
                                           :autoReloadType "Amount",
                                           :day 0,
                                           :paymentMethodId @payment-method-id
                                           :status "active",
                                           :triggerAmount 149.00,
                                           }
                                          )
                                   :content-type :json})
                      (print-body)
                      :status))))

      (let [path (bidi/path-for r ::card-reload/check :card-number (:cardNumber card))
            res @(http/get (format "http://localhost:%s%s"  port path)
                           {:throw-exceptions false
                            :body-encoding "UTF-8"

                            :content-type :json})
            body (parse-body res)
            ]
        (is (= 200 (-> res :status)))
;        (is (= nil body))
        (let [mails @(:mails (:mailer *system*))]
          (is (= 2 (count mails)))))

      (let [path (bidi/path-for r ::card/autoreload :card-id card-id)]
        (is (= 200(-> @(http/post (format "http://localhost:%s%s?access_token=%s"  port path *user-access-token*)
                                  {:throw-exceptions false
                                   :body-encoding "UTF-8"
                                   :body (json/generate-string
                                          {
                                           :amount 151.00,
                                           :autoReloadType "Amount",
                                           :day 0,
                                           :paymentMethodId @payment-method-id
                                           :status "active",
                                           :triggerAmount 151.00,
                                           }
                                          )
                                   :content-type :json})
                      (print-body)
                      :status))))


      (let [path (bidi/path-for r ::card-reload/check :card-number (:cardNumber card))
            res @(http/get (format "http://localhost:%s%s"  port path)
                           {:throw-exceptions false
                            :body-encoding "UTF-8"

                            :content-type :json})
            body (parse-body res)
            ]

        (is (= 200 (-> res :status)))
        (is (-> body :card :autoReloadProfile :active))
        (is (= (:cardNumber card) (:card-number body)))
        (is (= card-id (-> body :card :cardId)))
        (is (-> body :payment-data))
        (let [mails @(:mails (:mailer *system*))]

          (is (= 3 (count mails)))
          (is (= (select-keys (last mails) [:subject]) {:subject "Auto-Reload: your card has been topped up."}))
          )
))


    ))

(ns rebujito.api.resources.payment-test
  (:require
   [schema-generators.generators :as g]
   [byte-streams :as bs]
   [bidi.bidi :as bidi]
   [rebujito.config :refer (config)]
   [rebujito.protocols :as p]
   [schema-generators.generators :as g]
   [cheshire.core :as json]
   [manifold.deferred :as d]
   [rebujito.protocols :as p]
   [rebujito.api-test :refer (print-body)]
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
          payment-method-id (atom "")]
      (let [path (get-path ::payment/methods)]
       ;;         (println (format "http://localhost:%s%s?access_token=%s"  port path 123))
       (is (= 200 (-> @(http/get (format "http://localhost:%s%s?access_token=%s"  port path *user-access-token*)
                                 {:throw-exceptions false
                                  :body-encoding "UTF-8"
                                  :content-type :json})
                      (print-body)
                      :status)))



       (println path )
       (let [{:keys [status body]}
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
             body (-> (bs/to-string body)
                      (json/parse-string true))
             _          (is (= status 200))

             ]


         (clojure.pprint/pprint body)
         (is  (:paymentMethodId body))
         (reset! payment-method-id (:paymentMethodId body))
         ))

      ;; ::card/reload

      (let [api-id ::card/reload
            r (-> *system* :docsite-router :routes)
            path (bidi/path-for r api-id :card-id 123)]
        ;;         (println (format "http://localhost:%s%s?access_token=%s"  port path 123))
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
            path (bidi/path-for r api-id :card-id 123)]
        ;;         (println (format "http://localhost:%s%s?access_token=%s"  port path 123))
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



        (let [ar (:autoReload (p/find (-> *system* :user-store) (:_id (p/read-token (-> *system* :authenticator) *user-access-token*) )))]
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
                     (json/parse-string true)) ["Missing or invalid auto reload amount attribute is required. Amount must be within the range of 10-100"]))))

     (let [api-id ::card/autoreload-disable
            r (-> *system* :docsite-router :routes)
            path (bidi/path-for r api-id :card-id 123)]
        ;;         (println (format "http://localhost:%s%s?access_token=%s"  port path 123))
        (is (= 200(-> @(http/put (format "http://localhost:%s%s?access_token=%s"  port path *user-access-token*)
                                  {:throw-exceptions false
                                   :body-encoding "UTF-8"
                                   :content-type :json})
                      (print-body)
                      :status)))



        (let [ar (:autoReload (p/find (-> *system* :user-store) (:_id (p/read-token (-> *system* :authenticator) *user-access-token*) )))]
          (is (false? (:active ar)))))



      )
    )




  )

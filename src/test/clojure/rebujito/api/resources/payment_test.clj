(ns rebujito.api.resources.payment-test
  (:require
   [schema-generators.generators :as g]
   [byte-streams :as bs]
   [bidi.bidi :as bidi]
   [rebujito.config :refer (config)]
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




       (let [{:keys [status body]}
             (-> @(http/post (format "http://localhost:%s%s?access_token=%s"  port path *user-access-token*)
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
             _          (is (= status 200))
             body (-> (bs/to-string body)
                      (json/parse-string true))
             ]


         (clojure.pprint/pprint body)
         (is  (:paymentMethodId body))
         (reset! payment-method-id (:paymentMethodId body))
;         (clojure.pprint/pprint (p/find (-> *system* :user-store) (:_id (p/read-token (:authenticator *system*) *user-access-token*))))
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
                      (print-body)
                      :status)))))
    )




  )

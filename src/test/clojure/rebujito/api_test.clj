(ns rebujito.api-test
  (:require

   [aleph.http :as http]
   [bidi.bidi :as bidi]
   [byte-streams :as bs]
   [cheshire.core :as json]
   [clj-http.client :as http-c]
   [clojure.pprint :refer (pprint)]
   [clojure.test :refer :all]
   [manifold.deferred :as d]
   [org.httpkit.client :as http-k]
   [rebujito.base-test :refer (system-fixture *system* api-config new-account-sb *app-access-token* *user-account-data* *user-access-token*
                                              new-sig get-path access-token-application
                                              access-token-user)]
   [rebujito.api.resources
    [account :as account]
    [card :as card]
    [devices :as devices]
    [login :as login]
    [oauth :as oauth]
    [payment :as payment]
    [profile :as profile]
    [social-profile :as social-profile]]

   [rebujito.api.sig :as api-sig]
   [rebujito.api.time :as api-time]
   [rebujito.config :refer (config)]
   [rebujito.logging :as log-levels]
   [rebujito.store.mocks :as mocks]
   [rebujito.system :refer (new-production-system)]
   [schema-generators.generators :as g]
   [schema.core :as s]
   [taoensso.timbre :as log]

   ))

(log/set-config! log-levels/timbre-info-config)


(use-fixtures :each (system-fixture #{:+mock-mimi :+ephemeral-db}))




(defn print-body [c]
  (log/info ">>>>> ****"(-> c :body bs/to-string))
  c)

(defn oauth-login-data []
  (let [{:keys [key secret]} (api-config)]
   {:grant_type "password",
    :client_id key,
    :client_secret secret,
    :username "juanantonioruz@gmail.com",
    :password "real-secret",
    :scope "test_scope"}))

(deftest test-20*
  (time
   (let [r (-> *system* :docsite-router :routes)
         port (-> *system*  :webserver :port)]

     (testing ::devices/register
       (let [path (get-path ::devices/register)]
                                        ;         (println (format "http://localhost:%s%s?access_token=%s"  port path 123))
         (is (= 202 (-> @(http/post (format "http://localhost:%s%s?access_token=%s"  port path 123)
                                    {:throw-exceptions false
                                     :body-encoding "UTF-8"
                                     :body (json/generate-string (g/generate (-> devices/schema :post )))
                                     :content-type :json})
                                        ;                        print-body
                        :status)))))

     (testing ::payment/method-detail
       (let [api-id ::payment/method-detail
             path (bidi/path-for r api-id :payment-method-id 12345)]
         ;;         (println (format "http://localhost:%s%s?access_token=%s"  port path 123))
         (is (= 200 (-> @(http/get (format "http://localhost:%s%s?access_token=%s"  port path 123)
                                   {:throw-exceptions false
                                    :body-encoding "UTF-8"
                                    :content-type :json})
                        :status)))

         ;; can't test until we stop stubbing, needs real token ID
         #_(is (= 200 (-> @(http/delete (format "http://localhost:%s%s?access_token=%s"  port path 123)
                                        {:throw-exceptions false
                                         :body-encoding "UTF-8"
                                         :content-type :json})
                          :status)))

         #_(let [{:keys [status body]}
                 (-> @(http/put (format "http://localhost:%s%s?access_token=%s"  port path 123)
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
                                 :content-type :json})
                     )]
             (is (= status 200))
             (is (.contains (slurp body) "paymentMethodId"))
             )
         ))

     (testing ::profile/me
       (let [api-id ::profile/me
             path (bidi/path-for r api-id)]
         (is (= 200 (-> @(http/get (format "http://localhost:%s%s?access_token=%s"  port path *user-access-token*)
                                   {:throw-exceptions false
                                    :body-encoding "UTF-8"
                                    :content-type :json})
                        print-body
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
                                :status)))))

     (testing ::login/validate-password
       (let [api-id ::login/validate-password
             path (bidi/path-for r api-id)]
         (is (= 200 (-> @(http/post (format "http://localhost:%s%s?access_token=%s"  port path *user-access-token*)
                                    {:throw-exceptions false
                                     :body-encoding "UTF-8"
                                     :body (json/generate-string
                                            (assoc (g/generate (-> login/schema :validate-password :post)) :password (:password *user-account-data*)))
                                     :content-type :json})
                                        ;                        print-body
                        :status)))

         ))

     (testing ::login/forgot-password
       (let [api-id ::login/forgot-password
             path (bidi/path-for r api-id)
                                        ;       access_token (access-token-user (:emailAddress account-data) (:password account-data))
             ]
         (-> @(http/post (format "http://localhost:%s%s?access_token=%s"  port path *user-access-token*)
                         {:throw-exceptions false
                          :body-encoding "UTF-8"
                          :body (json/generate-string
                                 (select-keys *user-account-data*  (keys (-> login/schema :forgot-password :post))))
                          :content-type :json})
                                        ;             print-body
             :status)
         #_(is (= 200 ))

         ))

     )))


(comment (http-c/post "https://api.swarmloyalty.co.za/mimi/starbucks/account"
              {:throw-exceptions false
               :insecure? true
               :headers {"Authorization" (format "Bearer %s" (:token (:mimi (config :prod))))}
               :form-params {:birth {:dayOfMonth "7", :month "10"}, :city "Zaandam", :email "juanito@uno.com", :firstname "Juanito", :lastname "Bezoe", :postalcode "1506ZL", :region "Android"}
               :body-encoding "UTF-8"
               :content-type :json})

         )


(comment (let [{:keys [status body]} (http-c/get (format "https://api.swarmloyalty.co.za/mimi/starbucks/account/%s/balances"  "9623570900002")
                                         {:headers {"Authorization" (format "Bearer %s" (:token (:mimi (config :prod))))}
                                          :insecure? true
                                          :content-type :json
                                          :accept :json
                                          :as :json
                                          :throw-exceptions true
                                          :form-params {}})]
   body
   ))

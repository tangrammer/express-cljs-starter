(ns rebujito.api-test
  (:require
   [buddy.sign.util :refer (to-timestamp)]
   [rebujito.api.sig :as api-sig]
   [rebujito.api.time :as api-time]
   [aleph.http :as http]
   [org.httpkit.client :as http-k]
   [clj-http.client :as http-c]
   [bidi.bidi :as bidi]
   [byte-streams :as bs]
   [cheshire.core :as json]
   [clojure.pprint :refer (pprint)]
   [clojure.test :refer :all]
   [com.stuartsierra.component :as component]
   [rebujito.api.resources.account :as account]
   [rebujito.api.resources.profile :as profile]
   [rebujito.api.resources.card :as card]
   [rebujito.api.resources.oauth :as oauth]
   [rebujito.api.resources.login :as login]
   [rebujito.store.mocks :as mocks]
   [rebujito.api.resources.payment :as payment]
   [rebujito.api.resources.social-profile :as social-profile]
   [rebujito.config :refer (config)]
   [rebujito.system :refer (new-production-system)]
   [schema-generators.generators :as g]
   [schema.core :as s]
   [manifold.deferred :as d]
   [rebujito.logging :as log-levels]
   [taoensso.timbre :as log]
   [rebujito.system.dev-system :as dev]
   ))

(log/set-config! log-levels/timbre-info-config)

(def ^:dynamic *system* nil)

(defmacro with-system [system & body]
  `(let [s# (component/start ~system)]
     (try
       (binding [*system* s#] ~@body)
       (finally
         (component/stop s#)))))

(defn system-fixture [config-env]
  (fn[f]
    (with-system (-> (dev/new-dev-system config-env (update-in (config :test) [:yada :port]
                                                               (comp inc (fn [s]
                                                                           (if (= String (type s))
                                                                             (read-string s)
                                                                             s) )))))
      (try
        (s/with-fn-validation
          (f))
        (catch Exception e (do (println (str "caught exception: " (.getMessage e)))
                               (throw e)))))))

(use-fixtures :each (system-fixture #{:+mock-mimi}))

(defn api-config []
  (-> (config :test) :api))

(defn new-sig []
  (let [{:keys [key secret]} (api-config)
        t (api-time/now)]
    (println ">>>>" (to-timestamp t))
    (api-sig/new-sig t key secret)))

(defn print-body [c]
  (log/info ">>>>> ****"(-> c :body bs/to-string))
  c
  )
(defn oauth-login-data []
  (let [{:keys [key secret]} (api-config)]
   {:grant_type "password",
    :client_id key,
    :client_secret secret,
    :username "juanantonioruz@gmail.com",
    :password "real-secret",
    :scope "test_scope"}))


(defn new-account-sb []
{:countrySubdivision "aa",
 :registrationSource "aa",
 :addressLine1 "zz",
 :password "real-secret",
 :emailAddress "hola@hola.com",
 :city "Sevilla",
 :firstName "Juan",
 :birthDay "13",
 :birthMonth "06",
 :lastName "Ruz",
 :receiveStarbucksEmailCommunications "ok",
 :postalCode "41003",
 :country "Spain"
 :userName "juan"}
  )



#_(deftest test-token
  (time
   (let [r (-> *system* :docsite-router :routes)
         port (-> *system*  :webserver :port)
         new-account (g/generate (:post account/schema))
         new-account (new-account-sb)
         sig (new-sig)
         access_token (atom "")]

     #_(testing ::account/create
       (let [api-id ::account/create
             path (bidi/path-for r api-id)]
         (println (format "http://localhost:%s%s?access_token=%s&market=%s"  port path 123 1234))
         (is (= 201  (-> @(http/post (format "http://localhost:%s%s?access_token=%s&market=%s"  port path 123 1234)
                                        {:throw-exceptions false
                                         :body (json/generate-string
                                                (assoc new-account

                                                       :birthDay "1"
                                                       :birthMonth "1"
                                                       ))
                                         :body-encoding "UTF-8"
                                         :content-type :json})
                         print-body
                            :status)))))

     (testing ::oauth/token-resource-owner
       (let [api-id ::oauth/token-resource-owner
             path (bidi/path-for r api-id)]

         ;; body conform :token-refresh-token schema
         (is (= 201 (-> @(http/post (format "http://localhost:%s%s?sig=%s"  port path sig)
                                    {:throw-exceptions false
                                     :body  (json/generate-string
                                             (assoc (g/generate (-> oauth/schema :token-refresh-token))
                                                    :grant_type "refresh_token"
                                                    :client_id (:key (api-config))
                                                    :client_secret (:secret (api-config))))

                                     :body-encoding "UTF-8"
                                     :content-type :json})
                        print-body
                        :status)))
         ;; body conform :token-resource-owner schema
         ;; :grant_type "password"
         (is (= 201 (-> (let [r @(http/post (format "http://localhost:%s%s?sig=%s"  port path sig)
                                    {:throw-exceptions false
                                     :body (json/generate-string
                                            (assoc (g/generate (-> oauth/schema :token-resource-owner))
                                                   :grant_type "password"
                                                   :client_id (:key (api-config))
                                                   :client_secret (:secret (api-config))
                                                   :username (:emailAddress new-account)
                                                   :password (:password new-account)
                                                   ))
                                     :body-encoding "UTF-8"
                                     :content-type :json})
                              body (-> r :body bs/to-string (json/parse-string true))
                              _ (println body)

                              ]
                          (reset! access_token (:access_token body))
                          (println "\n >>>> password access_token "@access_token "\n")
                          r)
                        :status)))

         ;; body conform :token-resource-owner schema
         ;; :grant_type ""client_credentials""
         (is (= 201 (-> (let [r @(http/post (format "http://localhost:%s%s?sig=%s"  port path sig)
                                            {:throw-exceptions false
                                             :body (json/generate-string
                                                    (assoc (g/generate (-> oauth/schema :token-client-credentials))
                                                           :grant_type "client_credentials"
                                                           :client_id (:key (api-config))
                                                           :client_secret (:secret (api-config))))
                                             :body-encoding "UTF-8"
                                             :content-type :json})
                              body (-> r :body bs/to-string (json/parse-string true))
                              _ (println body)

                              ]
                          (reset! access_token (:access_token body))
                          (println "\n >>>> password client_credentials "@access_token "\n")
                          r)
                        :status)))
         ;; body conform :token-resource-owner schema
         ;; :grant_type "refresh_token"
         (is (= 201 (-> (let [r @(http/post (format "http://localhost:%s%s?sig=%s"  port path sig)
                                            {:throw-exceptions false
                                             :body (json/generate-string
                                                    (assoc (g/generate (-> oauth/schema :token-resource-owner))
                                                           :grant_type "refresh_token"
                                                           :client_id (:key (api-config))
                                                           :client_secret (:secret (api-config))
                                                           :username (:emailAddress new-account)
                                                           :password (:password new-account)
                                                           ))
                                             :body-encoding "UTF-8"
                                             :content-type :json})
                              body (-> r :body bs/to-string (json/parse-string true))
                              _ (println body)

                              ]
                          (reset! access_token (:access_token body))
                          (println "\n >>>> password refresh_token "@access_token "\n")
                          r)
                        :status)))

         ))))


     )



(defn access-token-application []
  (let [r (-> *system* :docsite-router :routes)
        port (-> *system*  :webserver :port)
        new-account (g/generate (:post account/schema))
        new-account (new-account-sb)
        sig (new-sig)
        access_token (atom "")]

    (let [api-id ::oauth/token-resource-owner
          path (bidi/path-for r api-id)]

      ;; :grant_type ""client_credentials""
      (is (= 201 (-> (let [r @(http/post (format "http://localhost:%s%s?sig=%s"  port path sig)
                                         {:throw-exceptions false
                                          :body (json/generate-string
                                                 (assoc (g/generate (-> oauth/schema :token-client-credentials))
                                                        :grant_type "client_credentials"
                                                        :client_id (:key (api-config))
                                                        :client_secret (:secret (api-config))))
                                          :body-encoding "UTF-8"
                                          :content-type :json})
                           body (-> r :body bs/to-string (json/parse-string true))
                                        ;                                            _ (println body)

                           ]
                       (reset! access_token (:access_token body))
                       r)
                     :status)))
      )
    @access_token))


(defn access-token-user [username password]
  (let [r (-> *system* :docsite-router :routes)
        port (-> *system*  :webserver :port)
        new-account (g/generate (:post account/schema))
        new-account (new-account-sb)
        sig (new-sig)
        access_token (atom "")]

    (let [api-id ::oauth/token-resource-owner
          path (bidi/path-for r api-id)]

      (-> (let [r @(http/post (format "http://localhost:%s%s?sig=%s"  port path sig)
                              {:throw-exceptions false
                               :body (json/generate-string
                                      (assoc (g/generate (-> oauth/schema :token-resource-owner))
                                             :grant_type "password"
                                             :client_id (:key (api-config))
                                             :client_secret (:secret (api-config))
                                             :username username
                                             :password password
                                             ))
                               :body-encoding "UTF-8"
                               :content-type :json})
                body (-> r :body bs/to-string (json/parse-string true))
                ;;                  _ (println body)

                ]
            (reset! access_token (:access_token body))
            (println "\n >>>> password access_token "@access_token "\n")
            r)
          :status)

      )
    @access_token))

(defn get-path [kw]
  (let [r (-> *system* :docsite-router :routes)
        sig (new-sig)
        api-id kw]
    (bidi/path-for r api-id)))

(deftest access-token-application*
  (testing "access resource protected with application role"
    (testing ::account/create
      (let [path (get-path ::account/create)
            port (-> *system*  :webserver :port)
            access_token (access-token-application)
            new-account (g/generate (:post account/schema))
            url (format "http://localhost:%s%s?access_token=%s&market=%s"  port path access_token 1234)]
        (is (= 201  (-> @(http/post url
                                    {:throw-exceptions false
                                     :body (json/generate-string
                                            (assoc new-account
                                                   :birthDay "1"
                                                   :birthMonth "1"
                                                   ))
                                     :body-encoding "UTF-8"
                                     :content-type :json})
                        print-body
                        :status)))
        (is (= 401  (-> @(http/post (format "http://localhost:%s%s?access_token=%s&market=%s"  port path "wrong_access_token" 1234)
                                    {:throw-exceptions false
                                     :body (json/generate-string
                                            (assoc new-account
                                                   :birthDay "1"
                                                   :birthMonth "1"
                                                   ))
                                     :body-encoding "UTF-8"
                                     :content-type :json})
                        print-body
                        :status)))))
    )
  )


(defn create-account [account-data]
  (let [port (-> *system*  :webserver :port)
        path (get-path ::account/create)
        access_token (access-token-application)]
    (let [res @(http/post (format "http://localhost:%s%s?access_token=%s&market=%s"  port path access_token 1234)
                          {:throw-exceptions false
                           :body (json/generate-string account-data)
                           :body-encoding "UTF-8"
                           :content-type :json})]
      res
      (is (= 201 (:status res) ))
      (json/parse-string (bs/to-string (:body res)) true)
      )
)
  )


;; we need to create an user to test this one
(deftest test-get-user
  (time
   (testing ::account/get-user
     (let [account-data #_(g/generate (:post account/schema)) (assoc (new-account-sb)
                                                                     :birthDay "1"
                                                                     :birthMonth "1")
           account (create-account  account-data)
           port (-> *system*  :webserver :port)
           path (get-path ::account/get-user)
           access_token (access-token-user (:emailAddress account-data)(:password account-data))]
       (pprint account)
       (println access_token)


       (is (= 201  (-> @(http/get (format "http://localhost:%s%s?access_token=%s"  port path access_token)
                                    {:throw-exceptions false
                                     :body-encoding "UTF-8"
                                     :content-type :json})
                         print-body
                         :status)))
       )

     )


   ))


(deftest test-20*
  (time
   (let [r (-> *system* :docsite-router :routes)
         port (-> *system*  :webserver :port)
         new-account (g/generate (:post account/schema))
         new-account (new-account-sb)
         sig (new-sig)
         access_token (atom "")]

     (testing ::account/create
       (let [path (get-path ::account/create)
             access_token (access-token-application)]
         (is (= 201  (-> @(http/post (format "http://localhost:%s%s?access_token=%s&market=%s"  port path access_token 1234)
                                        {:throw-exceptions false
                                         :body (json/generate-string
                                                (assoc new-account
                                                       ;; string or int should both work here
                                                       :birthDay "1"
                                                       :birthMonth 1
                                                       ))
                                         :body-encoding "UTF-8"
                                         :content-type :json})
                         print-body
                            :status)))))

     (testing ::oauth/token-resource-owner
       (let [path (get-path ::oauth/token-resource-owner) ]

         ;; body conform :token-refresh-token schema
         ;; :grant_type "refresh_token"
         (is (= 201 (-> @(http/post (format "http://localhost:%s%s?sig=%s"  port path sig)
                                    {:throw-exceptions false
                                     :body  (json/generate-string
                                             (assoc (g/generate (-> oauth/schema :token-refresh-token))
                                                    :grant_type "refresh_token"
                                                    :client_id (:key (api-config))
                                                    :client_secret (:secret (api-config))))

                                     :body-encoding "UTF-8"
                                     :content-type :json})
          ;;              print-body
                        :status)))

         ;; body conform :token-resource-owner schema
         ;; :grant_type "password"
         (is (= 201 (-> (let [r @(http/post (format "http://localhost:%s%s?sig=%s"  port path sig)
                                    {:throw-exceptions false
                                     :body (json/generate-string
                                            (assoc (g/generate (-> oauth/schema :token-resource-owner))
                                                   :grant_type "password"
                                                   :client_id (:key (api-config))
                                                   :client_secret (:secret (api-config))
                                                   :username (:emailAddress new-account)
                                                   :password (:password new-account)
                                                   ))
                                     :body-encoding "UTF-8"
                                     :content-type :json})
                              body (-> r :body bs/to-string (json/parse-string true))
            ;;                  _ (println body)

                              ]
                          (reset! access_token (:access_token body))
                          (println "\n >>>> password access_token "@access_token "\n")
                          r)
                        :status)))

         ;; body conform :token-client-credentials schema
         ;; :grant_type "client_credentials"
         (is (= 201 (-> (let [r @(http/post (format "http://localhost:%s%s?sig=%s"  port path sig)
                                            {:throw-exceptions false
                                             :body (json/generate-string
                                                    (assoc (g/generate (-> oauth/schema :token-client-credentials))
                                                           :grant_type "client_credentials"
                                                           :client_id (:key (api-config))
                                                           :client_secret (:secret (api-config))))
                                             :body-encoding "UTF-8"
                                             :content-type :json})
                              body (-> r :body bs/to-string (json/parse-string true))
              ;;                _ (println body)

                              ]
                          r)
                        :status)))))

     ;;     (println @access_token)

     (testing ::card/get-cards
       (let [path (get-path ::card/get-cards)]
         (is (= 201 (-> @(http/get (format "http://localhost:%s%s?access_token=%s"  port path 123)
                                   {:throw-exceptions false
                                    :body-encoding "UTF-8"
                                    :content-type :json})
                        :status)))))

     (testing ::card/register-physical
       (let [path (get-path ::card/register-physical)]
         (println (format "http://localhost:%s%s?access_token=%s"  port path 123))
         (is (= 200 (-> @(http/post (format "http://localhost:%s%s?access_token=%s"  port path 123)
                                    {:throw-exceptions false
                                     :body-encoding "UTF-8"
                                     :body (json/generate-string
                                            (assoc (g/generate (-> card/schema :post :register-physical))
                                                   :cardNumber (str (+ (rand-int 1000) (read-string (format "96235709%05d" 0)))))
                                            )
                                     :content-type :json})
                        print-body
                        :status)))))

     (testing ::card/register-digital-cards
       (let [path (get-path ::card/register-digital-cards)]
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

     (testing ::payment/reload
       (let [api-id ::payment/reload
             path (bidi/path-for r api-id :card-id 123)]
         (println (format "http://localhost:%s%s?access_token=%s"  port path 123))
         (is (= 200(-> @(http/post (format "http://localhost:%s%s?access_token=%s"  port path 123)
                                      {:throw-exceptions false
                                       :body-encoding "UTF-8"
                                       :body (json/generate-string
                                               {
                                                :amount 15
                                                :paymentMethodId "1234567"
                                                :sessionId ""
                                                })
                                       :content-type :json})
                        :status)))))

     (testing ::payment/methods
       (let [path (get-path ::payment/methods)]
         (println (format "http://localhost:%s%s?access_token=%s"  port path 123))
         (is (= 200 (-> @(http/get (format "http://localhost:%s%s?access_token=%s"  port path 123)
                                   {:throw-exceptions false
                                    :body-encoding "UTF-8"
                                    :content-type :json})
                        :status)))
         (let [{:keys [status body]}
               (-> @(http/post (format "http://localhost:%s%s?access_token=%s"  port path 123)
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
           (is (= status 201))
           (is (.contains (slurp body) "paymentMethodId"))
           )))

     (testing ::payment/method-detail
       (let [api-id ::payment/method-detail
             path (bidi/path-for r api-id :payment-method-id 12345)]
         (println (format "http://localhost:%s%s?access_token=%s"  port path 123))
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
         (is (= 200 (-> @(http/get (format "http://localhost:%s%s?access_token=%s"  port path @access_token)
                                   {:throw-exceptions false
                                    :body-encoding "UTF-8"
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
                        :status)))))

     (testing ::login/validate-password
       (let [api-id ::login/validate-password
             path (bidi/path-for r api-id)]
         (println "@access_token" (json/generate-string
                                           (assoc (g/generate (-> login/schema :validate-password :post)) :password (:password new-account))))
         (is (= 200 (-> @(http/post (format "http://localhost:%s%s?access_token=%s"  port path @access_token)
                                   {:throw-exceptions false
                                    :body-encoding "UTF-8"
                                    :body (json/generate-string
                                           (assoc (g/generate (-> login/schema :validate-password :post)) :password (:password new-account)))
                                    :content-type :json})
                        print-body
                        :status)))

         ))
     (testing ::login/forgot-password
       (let [api-id ::login/forgot-password
             path (bidi/path-for r api-id)]
         (println "@access_token" @access_token)
         (is (= 200 (-> @(http/post (format "http://localhost:%s%s?access_token=%s"  port path @access_token)
                                   {:throw-exceptions false
                                    :body-encoding "UTF-8"
                                    :body (json/generate-string
                                           (select-keys new-account (keys (-> login/schema :forgot-password :post))))
                                    :content-type :json})
                        print-body
                        :status)))

         ))

     )))

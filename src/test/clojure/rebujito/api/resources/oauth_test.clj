(ns rebujito.api.resources.oauth-test
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
    [account :as account]
    [oauth :as oauth]
    [login :as login]]
   [clojure.test :refer :all]))


(use-fixtures :each (system-fixture #{:+mock-mimi :+ephemeral-db}))

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
;;                        print-body
                        :status)))
        (is (= 401  (-> @(http/post (format "http://localhost:%s%s?access_token=%s&market=%s"  port path "wrong_access_token" 1234)
                                    {:throw-exceptions false
                                     :form-params (assoc new-account
                                                        :birthDay "1"
                                                        :birthMonth "1")
                                     :body-encoding "UTF-8"
                                     :content-type :application/x-www-form-urlencoded})
;;                        print-body
                        :status)))))
    )
  )

(deftest test-token
  (time
   (let
     [r (-> *system* :docsite-router :routes)
      port (-> *system*  :webserver :port)

      account-data #_(g/generate (:post account/schema)) (assoc (new-account-sb)
                                                                :birthDay "1"
                                                                :birthMonth "1")
      new-account (create-account account-data)

      sig (new-sig)
      access_token (atom "")]

;;     (pprint  new-account)

     (testing ::oauth/token-resource-owner
       (let [api-id ::oauth/token-resource-owner
             path (bidi/path-for r api-id)]
         ;; body conform :token-refresh-token schema
         (is (= 201 (-> @(http/post (format "http://localhost:%s%s?sig=%s"  port path sig)
                                    {:throw-exceptions true
                                     :form-params
                                     (assoc (g/generate (-> oauth/schema :token-refresh-token))
                                            :grant_type "refresh_token"
                                            :client_id (:key (api-config))
                                            :client_secret (:secret (api-config)))

                                     :body-encoding "UTF-8"
                                     :content-type :application/x-www-form-urlencoded})
                      ;  print-body
                        :status)))
                  ;; body conform :token-resource-owner schema
         ;; :grant_type "password"
         (is (= 201 (-> (let [r @(http/post (format "http://localhost:%s%s?sig=%s"  port path sig)
                                    {:throw-exceptions false
                                     :form-params
                                     (assoc (g/generate (-> oauth/schema :token-resource-owner))
                                            :grant_type "password"
                                            :client_id (:key (api-config))
                                            :client_secret (:secret (api-config))
                                            :username (:emailAddress account-data)
                                            :password (:password account-data)
                                            )
                                     :body-encoding "UTF-8"
                                     :content-type :x-www-form-urlencoded})
                              body (-> r :body bs/to-string (json/parse-string true))
;;                              _ (println body)

                              ]
                          (reset! access_token (:access_token body))
;;                          (println "\n >>>> password access_token "@access_token "\n")
                          r)
                        :status)))

         ;; body conform :token-resource-owner schema
         ;; :grant_type ""client_credentials""
         (is (= 201 (-> (let [r @(http/post (format "http://localhost:%s%s?sig=%s"  port path sig)
                                            {:throw-exceptions false
                                             :form-params (assoc (g/generate (-> oauth/schema :token-client-credentials))
                                                                 :grant_type "client_credentials"
                                                                 :client_id (:key (api-config))
                                                                 :client_secret (:secret (api-config)))
                                             :body-encoding "UTF-8"
                                             :content-type :application/x-www-form-urlencoded})
                              body (-> r :body bs/to-string (json/parse-string true))
;;                              _ (println body)

                              ]
                          (reset! access_token (:access_token body))
;;                          (println "\n >>>> password client_credentials "@access_token "\n")
                          r)
                        :status)))
         ;; body conform :token-resource-owner schema
         ;; :grant_type "refresh_token"
         (is (= 201 (-> (let [r @(http/post (format "http://localhost:%s%s?sig=%s"  port path sig)
                                            {:throw-exceptions false
                                             :form-params
                                             (assoc (g/generate (-> oauth/schema :token-refresh-token))
                                                    :grant_type "refresh_token"
                                                    :client_id (:key (api-config))
                                                    :client_secret (:secret (api-config))

                                                    )
                                             :body-encoding "UTF-8"
                                             :content-type :application/x-www-form-urlencoded})
                              body (-> r :body bs/to-string (json/parse-string true))
;;                              _ (println body)

                              ]
                          (reset! access_token (:access_token body))
;;                          (println "\n >>>> password refresh_token "@access_token "\n")
                          r)
                        :status)))


         )))))

(deftest test-token-bis
  (let  [r (-> *system* :docsite-router :routes)
         port (-> *system*  :webserver :port)

         account-data #_(g/generate (:post account/schema)) (assoc (new-account-sb)
                                                                   :birthDay "1"
                                                                   :birthMonth "1")
         new-account (create-account account-data)

         sig (new-sig)
         access_token (atom "")]
    (testing ::oauth/token-resource-owner
      (let [path (get-path ::oauth/token-resource-owner) ]

        ;; body conform :token-refresh-token schema
        ;; :grant_type "refresh_token"
        (is (= 201 (-> @(http/post (format "http://localhost:%s%s?sig=%s"  port path sig)
                                   {:throw-exceptions false
                                    :form-params
                                    (assoc (g/generate (-> oauth/schema :token-refresh-token))
                                           :grant_type "refresh_token"
                                           :client_id (:key (api-config))
                                           :client_secret (:secret (api-config)))

                                    :body-encoding "UTF-8"
                                    :content-type :x-www-form-urlencoded})
                       ;;              print-body
                       :status)))

        ;; body conform :token-resource-owner schema
        ;; :grant_type "password"
        (is (= 201 (-> (let [r @(http/post (format "http://localhost:%s%s?sig=%s"  port path sig)
                                           {:throw-exceptions false
                                            :form-params
                                            (assoc (g/generate (-> oauth/schema :token-resource-owner))
                                                   :grant_type "password"
                                                   :client_id (:key (api-config))
                                                   :client_secret (:secret (api-config))
                                                   :username (:emailAddress new-account)
                                                   :password (:password account-data)
                                                   )
                                            :body-encoding "UTF-8"
                                            :content-type :x-www-form-urlencoded})
                             body (-> r :body bs/to-string (json/parse-string true))
                                               _ (println body)

                             ]
                         (reset! access_token (:access_token body))
                         ;;                          (println "\n >>>> password access_token "@access_token "\n")
                         r)
                       :status)))

        ;; body conform :token-client-credentials schema
        ;; :grant_type "client_credentials"
        (is (= 201 (-> (let [r @(http/post (format "http://localhost:%s%s?sig=%s"  port path sig)
                                           {:throw-exceptions false
                                            :form-params
                                            (assoc (g/generate (-> oauth/schema :token-client-credentials))
                                                   :grant_type "client_credentials"
                                                   :client_id (:key (api-config))
                                                   :client_secret (:secret (api-config)))
                                            :body-encoding "UTF-8"
                                            :content-type :x-www-form-urlencoded})
                             body (-> r :body bs/to-string (json/parse-string true))
                             ;;                _ (println body)

                             ]
                         r)
                       :status)))))))

(deftest test-create-account
  (testing "create-account-only"
    (create-account  (assoc (new-account-sb)
                            :birthDay "1"
                            :birthMonth "1"))))

(deftest test-get-user
  (time
   (testing ::account/me
     (let [account-data  (assoc (new-account-sb)
                                :birthDay "1"
                                :birthMonth "1")
           account (create-account  account-data)
           port (-> *system*  :webserver :port)
           path (get-path ::account/me)
           access_token (access-token-user (:emailAddress account-data)(:password account-data))]
;;       (pprint account)
;;       (println "access-token:::" access_token)


       (is (= 201  (-> @(http/get (format "http://localhost:%s%s?access_token=%s"  port path access_token)
                                    {:throw-exceptions false
                                     :body-encoding "UTF-8"
                                     :content-type :json})
;;                      print-body
                       :status)))

       (println access_token)
     (testing ::login/validate-password
       (let [path (get-path ::login/validate-password)]
         (is (= 200 (-> @(http/post (format "http://localhost:%s%s?access_token=%s"  port path access_token)
                                    {:throw-exceptions false
                                     :body-encoding "UTF-8"
                                     :body (json/generate-string
                                            (assoc (g/generate (-> login/schema :validate-password :post)) :password (:password account-data)))
                                     :content-type :json})
                        print-body
                        :status)))

         ))

       )

     )


   ))

(ns rebujito.api.resources.oauth-test
  (:require
   [clojure.pprint :refer (pprint)]
   [byte-streams :as bs]
   [bidi.bidi :as bidi]
   [rebujito.config :refer (config)]
   [schema-generators.generators :as g]
   [cheshire.core :as json]
   [manifold.deferred :as d]
   [rebujito.protocols :as p]
   [rebujito.api-test :refer (print-body)]
   [rebujito.base-test :refer (system-fixture *system* *user-access-token*
                                              *user-account-data* get-path  access-token-application access-token-user new-account-sb create-account new-sig  api-config)]
   [aleph.http :as http]
   [rebujito.api.resources
    [account :as account]
    [oauth :as oauth]
    [login :as login]]
   [clojure.test :refer :all]))


(use-fixtures :each (system-fixture #{:+mock-mimi :+ephemeral-db}))

(deftest access-token-application*
  "access resource protected with application role"
  (testing ::account/create
    (let [path (get-path ::account/create)
          port (-> *system*  :webserver :port)
          access_token (access-token-application)
          new-account (assoc (new-account-sb)
                   :birthDay "1"
                   :birthMonth "1")
          url (format "http://localhost:%s%s?access_token=%s&market=%s"  port path access_token 1234)]
      (is (= 201  (-> @(http/post url
                                  {:throw-exceptions false
                                   :body (json/generate-string new-account)
                                   :body-encoding "UTF-8"
                                   :content-type :json})
                      ;;                        print-body
                      :status)))
      (is (= 401  (-> @(http/post (format "http://localhost:%s%s?access_token=%s&market=%s"  port path "wrong_access_token" 1234)
                                  {:throw-exceptions false
                                   :form-params new-account
                                   :body-encoding "UTF-8"
                                   :content-type :application/x-www-form-urlencoded})
                      ;;                        print-body
                      :status)))
      ;; trying to create an account with same email should return 400
      (is (= 400  (-> @(http/post url
                                  {:throw-exceptions false
                                   :body (json/generate-string new-account)
                                   :body-encoding "UTF-8"
                                   :content-type :json})
                      ;;                        print-body
                      :status)))

      )))

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
      access_token (atom "")
      refresh_token (atom "")
      ]
     (testing ::oauth/token-resource-owner
       (let [api-id ::oauth/token-resource-owner
             path (bidi/path-for r api-id)]
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
                          (reset! refresh_token (:refresh_token body))
                          (reset! access_token (:access_token body))
                          ;; (println "\n >>>> password access_token "@access_token "\n")
                          ;; (println "\n >>>> password refresh_token "@refresh_token "\n")
;                         (println "password access-token " (:access_token body))
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
;                          (reset! access_token (:access_token body))
;;                          (println "\n >>>> password client_credentials "@access_token "\n")
                          r)
                        :status)))
         ;; :grant_type ""client_credentials"" wrong client_id invalid hex id
         (is (= 400 (-> (let [r @(http/post (format "http://localhost:%s%s?sig=%s"  port path sig)
                                            {:throw-exceptions false
                                             :form-params (assoc (g/generate (-> oauth/schema :token-client-credentials))
                                                                 :grant_type "client_credentials"
                                                                 :client_id "xxx"
                                                                 :client_secret "xxx")
                                             :body-encoding "UTF-8"
                                             :content-type :application/x-www-form-urlencoded})
                              body (-> r :body bs/to-string (json/parse-string true))]
                          (println body)
                          r)
                        :status)))

                  ;; :grant_type ""client_credentials"" good client_id invalid pw
         (is (= 400 (-> (let [r @(http/post (format "http://localhost:%s%s?sig=%s"  port path sig)
                                            {:throw-exceptions false
                                             :form-params (assoc (g/generate (-> oauth/schema :token-client-credentials))
                                                                 :grant_type "client_credentials"
                                                                 :client_id (:key (api-config))
                                                                 :client_secret "xxx")
                                             :body-encoding "UTF-8"
                                             :content-type :application/x-www-form-urlencoded})
                              body (-> r :body bs/to-string (json/parse-string true))]
                                                    (println body)
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
                                                    :refresh_token @refresh_token
                                                    )
                                             :body-encoding "UTF-8"
                                             :content-type :application/x-www-form-urlencoded})
                              body (-> r :body bs/to-string (json/parse-string true))
          ;                    _ (println "*******>>>***" r)

                              ]
                          ;;(reset! access_token (:access_token body))
                          ;;(println "\n >>>> password refresh_token "@access_token "\n")
;                          (println "refresh_token access-token " (:access_token body))
                          r)
                        :status)))

         ;; GET /logout
         (is (= 200 (-> (let [r @(http/get (format "http://localhost:%s%s?access_token=%s"  port (bidi/path-for r ::login/logout) @access_token)
                                            {:throw-exceptions false
                                             :body-encoding "UTF-8"
                                             :content-type :json})
                              body (-> r :body bs/to-string (json/parse-string true))
                                        ;                    _ (println "*******>>>***" r)

                              ]
                          ;;(reset! access_token (:access_token body))
                          ;;(println "\n >>>> password refresh_token "@access_token "\n")
                                        ;                          (println "refresh_token access-token " (:access_token body))
                          r)
                        :status)))

         ;; try refresh after logout => 403
         (is (= 403 (-> (let [r @(http/post (format "http://localhost:%s%s?sig=%s"  port path sig)
                                            {:throw-exceptions false
                                             :form-params
                                             (assoc (g/generate (-> oauth/schema :token-refresh-token))
                                                    :grant_type "refresh_token"
                                                    :client_id (:key (api-config))
                                                    :client_secret (:secret (api-config))
                                                    :refresh_token @refresh_token
                                                    )
                                             :body-encoding "UTF-8"
                                             :content-type :application/x-www-form-urlencoded})
                              body (-> r :body bs/to-string (json/parse-string true))
          ;                    _ (println "*******>>>***" r)

                              ]
                          ;;(reset! access_token (:access_token body))
                          ;;(println "\n >>>> password refresh_token "@access_token "\n")
;                          (println "refresh_token access-token " (:access_token body))
                          r)
                        :status)))
         )))))

(deftest test-get-user
  (time
   (testing ::account/me
     (let [port (-> *system*  :webserver :port)
           path (get-path ::account/me)]

       (is (= 201  (-> @(http/get (format "http://localhost:%s%s?access_token=%s"  port path *user-access-token*)
                                    {:throw-exceptions false
                                     :body-encoding "UTF-8"
                                     :content-type :json})
;;                      print-body
                       :status)))


     (testing ::login/validate-password
       (let [path (get-path ::login/validate-password)]
         (is (= 200 (-> @(http/post (format "http://localhost:%s%s?access_token=%s"  port path *user-access-token*)
                                    {:throw-exceptions false
                                     :body-encoding "UTF-8"
                                     :body (json/generate-string
                                            (assoc (g/generate (-> login/schema :validate-password :post)) :password (:password *user-account-data*)))
                                     :content-type :json})
;                        print-body
                        :status)))

         ))))))


(deftest test-JWT
  (time
   (testing "decrypt JWT"
     (let [token-data (p/read-token  (-> *system* :authenticator) *user-access-token*)]
       ;; (pprint token-data)
       ;; (println "----")
       ;; (pprint *user-account-data*)
       ;; (println "----")
       (is (pos? (count (clojure.set/intersection (set (p/read-token  (-> *system* :authenticator) *user-access-token*))
                                                  (set  *user-account-data*)))))))))

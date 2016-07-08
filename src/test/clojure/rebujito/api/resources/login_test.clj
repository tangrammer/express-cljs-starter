(ns rebujito.api.resources.login-test
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
   [rebujito.api-test :refer (print-body create-digital-card* parse-body)]
   [rebujito.base-test :refer (generate-mail system-fixture *system* *user-account-data* *app-access-token* *user-access-token* get-path  access-token-application access-token-user new-account-sb create-account new-sig  api-config)]
   [aleph.http :as http]
   [rebujito.api.resources
    [payment :as payment]
    [addresses :as addresses]
    [card :as card]
    [account :as account]
    [oauth :as oauth]
    [login :as login]]
   [clojure.test :refer :all]))


(use-fixtures :each (system-fixture #{:+mock-mimi :+ephemeral-db :+mock-mailer}))

(deftest login-testing
  (testing ::login/validate-password
    (let [port (-> *system*  :webserver :port)
          address-id (atom "")]

      ;; invalid password!
      (let [path (get-path ::login/validate-password)
            http-response @(http/post (format "http://localhost:%s%s?access_token=%s"  port path *user-access-token*)
                                     {:throw-exceptions false
                                      :body-encoding "UTF-8"
                                      :body (json/generate-string
                                             (g/generate (:post (:validate-password login/schema)))

                                         )
                                      :content-type :json})
            body (parse-body http-response)
            ]
        (is (= 403 (-> http-response :status)))
        (is (= {:message "Forbidden: password doesn't match"} body)))

      ;; valid password!
      (let [path (get-path ::login/validate-password)
            http-response @(http/post (format "http://localhost:%s%s?access_token=%s"  port path *user-access-token*)
                                     {:throw-exceptions false
                                      :body-encoding "UTF-8"
                                      :body (json/generate-string
                                             (assoc (g/generate (:post (:validate-password login/schema)))
                                                    :password (:password *user-account-data*))

                                         )
                                      :content-type :json})
            body (parse-body http-response)
            ]
        (is (= 200 (-> http-response :status)))
        (is (= nil body)))

      ;; valid password!
      (let [path (get-path ::login/validate-password)
            http-response @(http/post (format "http://localhost:%s%s?access_token=%s"  port path "xxx-invalid-user-access-token")
                                     {:throw-exceptions false
                                      :body-encoding "UTF-8"
                                      :body (json/generate-string
                                             (assoc (g/generate (:post (:validate-password login/schema)))
                                                    :password (:password *user-account-data*))

                                         )
                                      :content-type :json})
            body (parse-body http-response)
            ]
        (is (= 401 (-> http-response :status)))
        (is (= {:status 401,
                :message "Unauthorized",
                :id "rebujito.api.resources.login/validate-password",
                :error
                {:error
                 "clojure.lang.ExceptionInfo: No authorization provided {:status 401, :headers {}}",
                 :data "{:status 401, :headers {}}"}} body)))


      ))




  )


(deftest forgot-password

  (testing ::login/forgot-password
    (let [port (-> *system*  :webserver :port)
          address-id (atom "")]

      (let [path (get-path ::login/forgot-password)
            http-response @(http/post (format "http://localhost:%s%s?access_token=%s"  port path *app-access-token*)
                                     {:throw-exceptions false
                                      :body-encoding "UTF-8"
                                      :body (json/generate-string
                                             (assoc (g/generate (:post (:forgot-password login/schema)))
                                                    :emailAddress (:emailAddress *user-account-data*)))
                                      :content-type :json})
            body (parse-body http-response)
            ]
        (is (= 200 (-> http-response :status)))
        (is (= nil body)))
        (let [mails @(:mails (:mailer *system*))]
          (is (= 3 (count mails)))

          (is (.contains (:subject (last mails)) "Reset your Starbucks Rewards Password")  )
          (is (p/read-token (:authenticator *system*)

                            (last (clojure.string/split
                                   (-> mails first :hidden) #"/"))
                            )  )
          #_(is (p/verify (:authorizer *system*) (:content (first mails)) rebujito.scopes/reset-password))))))

(deftest change-username  ;; => emailAddress

  (testing ::login/me-change-email
    (let [port (-> *system*  :webserver :port)
          send-token (atom "")
          new-email (generate-mail "juanantonioruz+%s@gmail.com")]

      (let [path (get-path ::login/me-change-email)
            http-response @(http/post (format "http://localhost:%s%s?access_token=%s"  port path *user-access-token*)
                                     {:throw-exceptions false
                                      :body-encoding "UTF-8"
                                      :body (json/generate-string
                                             (assoc (g/generate (:post (:change-email login/schema)))
                                                    :password  (:password *user-account-data*)
                                                    :new-email new-email))
                                      :content-type :json})
;            body (parse-body http-response)
            ]

        (is (= 200 (-> http-response :status)))
        (is (= "" (-> http-response :body bs/to-string)))


        (is (-> *system* :mailer :mails deref last :hidden))

        (reset! send-token (last (clojure.string/split
                                   (-> *system* :mailer :mails deref last :hidden) #"/")))
        )


      (let [path (get-path ::login/change-email)

            http-response @(http/put (format "http://localhost:%s%s?access_token=%s"  port path @send-token)
                                     {:throw-exceptions false
                                      :body-encoding "UTF-8"
                                      :content-type :json})]

        (is (= 200 (-> http-response :status)))
        (is (= "" (-> http-response :body bs/to-string)))
        (let [user-by-email (first (p/find (-> *system* :user-store) {:emailAddress new-email}))]
          (is (=  (-> *user-account-data*
                      (dissoc  :_id :password :birthDay :birthMonth)
                      (assoc  :emailAddress new-email :verifiedEmail true ))
                  (dissoc user-by-email :_id :password :birthMonth :birthDay :createdDate)))))


      ;; OPT checks
      (let [path (get-path ::login/change-email)

            http-response @(http/put (format "http://localhost:%s%s?access_token=%s"  port path @send-token)
                                     {:throw-exceptions false
                                      :body-encoding "UTF-8"
                                      :content-type :json})]

        (is (= 401 (-> http-response :status))))

      )))



(deftest me-change-password
  (testing ::login/me-change-password
    (let [port (-> *system*  :webserver :port)
          password (:password *user-account-data*)
          new-password "new-one:-)"]


      (let [path (get-path ::login/me-change-password)
            http-response @(http/post (format "http://localhost:%s%s?access_token=%s"  port path *user-access-token*)
                                     {:throw-exceptions false
                                      :body-encoding "UTF-8"
                                      :body (json/generate-string
                                             (assoc (g/generate (:post (:me-change-password login/schema)))
                                                    :password password
                                                    :new_password new-password)

                                         )
                                      :content-type :json})
            body (parse-body http-response)
            ]
        (is (= 200 (-> http-response :status)))
        (is (= nil body)))



      (let [path (get-path ::login/validate-password)
            http-response @(http/post (format "http://localhost:%s%s?access_token=%s"  port path *user-access-token*)
                                     {:throw-exceptions false
                                      :body-encoding "UTF-8"
                                      :body (json/generate-string
                                             (assoc (g/generate (:post (:validate-password login/schema)))
                                                    :password new-password)

                                         )
                                      :content-type :json})
            body (parse-body http-response)
            ]
        (is (= 200 (-> http-response :status)))
        (is (= nil body)))


      (let [path (get-path ::login/me-change-password)
            http-response @(http/post (format "http://localhost:%s%s?access_token=%s"  port path *user-access-token*)
                                     {:throw-exceptions false
                                      :body-encoding "UTF-8"
                                      :body (json/generate-string
                                             (assoc (g/generate (:post (:me-change-password login/schema)))
                                                    :password "xxx"
                                                    :new_password new-password)

                                         )
                                      :content-type :json})
            body (parse-body http-response)
            ]
        (is (= 403 (-> http-response :status)))
        (is (= {:message "Forbidden: password doesn't match"} body)))




      )))



(deftest change-pw

  (testing ::login/set-new-password
    (let [port (-> *system*  :webserver :port)
          new-password "12345"]

      (let [path (get-path ::login/forgot-password)
            http-response @(http/post (format "http://localhost:%s%s?access_token=%s"  port path *app-access-token*)
                                     {:throw-exceptions false
                                      :body-encoding "UTF-8"
                                      :body (json/generate-string
                                             (assoc (g/generate (:post (:forgot-password login/schema)))
                                                    :emailAddress (:emailAddress *user-account-data*)))
                                      :content-type :json})
            body (parse-body http-response)
            ]

        (is (= 200 (:status http-response)))
        )

      (is (-> *system* :mailer :mails deref last :hidden))

      (let [path (get-path ::login/set-new-password)
            data (assoc (g/generate (:put (:set-new-password login/schema)))
                        :new_password new-password)
            http-response @(http/put (format "http://localhost:%s%s?access_token=%s"  port path
                                             (last (butlast (clojure.string/split
                                                             (-> *system* :mailer :mails deref last :hidden) #"/"))))
                                     {:throw-exceptions false
                                      :body-encoding "UTF-8"
                                      :body (json/generate-string data)
                                      :content-type :json})
            body (parse-body http-response)]
        (is (= 200 (:status http-response))))

      (let [path (get-path ::login/set-new-password)
            data (g/generate (:put (:set-new-password login/schema)))
            http-response @(http/put (format "http://localhost:%s%s?access_token=%s"  port path (-> *system* :mailer :mails deref first :content))
                                     {:throw-exceptions false
                                      :body-encoding "UTF-8"
                                      :body (json/generate-string data)
                                      :content-type :json})
            body (parse-body http-response)]
        (is (= 401 (:status http-response))))

      (let [path (get-path ::login/validate-password)
            http-response @(http/post (format "http://localhost:%s%s?access_token=%s"  port path *user-access-token*)
                                     {:throw-exceptions false
                                      :body-encoding "UTF-8"
                                      :body (json/generate-string
                                             (assoc (g/generate (:post (:validate-password login/schema)))
                                                    :password new-password)

                                         )
                                      :content-type :json})
            body (parse-body http-response)
            ]
        (is (= 200 (-> http-response :status)))
        (is (= nil body)))
 )))

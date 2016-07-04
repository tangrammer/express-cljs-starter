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


(deftest reset-pw

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
          (is (= 2 (count mails)))
          (is (.contains (:subject (last mails)) "sending forgot-password")  )
          (is (p/read-token (:authenticator *system*) (:content (first mails)))  )
          #_(is (p/verify (:authorizer *system*) (:content (first mails)) rebujito.scopes/reset-password))))))

(deftest change-username  ;; => emailAddress

  (testing ::login/change-username
    (let [port (-> *system*  :webserver :port)
          send-token (atom "")]

      (let [path (get-path ::login/reset-username)
            http-response @(http/post (format "http://localhost:%s%s?access_token=%s"  port path *user-access-token*)
                                     {:throw-exceptions false
                                      :body-encoding "UTF-8"
                                      :body (json/generate-string
                                             (assoc (g/generate (:post (:reset-username login/schema)))
                                                    :password  (:password *user-account-data*)))
                                      :content-type :json})
;            body (parse-body http-response)
            ]

        (is (= 200 (-> http-response :status)))
        (is (= "" (-> http-response :body bs/to-string)))

        (is (-> *system* :mailer :mails deref first :content))

        (reset! send-token (-> *system* :mailer :mails deref last :content))
        )


      (let [path (get-path ::login/change-username)
            new-username (generate-mail "juanantonioruz+%s@gmail.com")
            http-response @(http/put (format "http://localhost:%s%s?access_token=%s"  port path @send-token)
                                     {:throw-exceptions false
                                      :body-encoding "UTF-8"
                                      :body (json/generate-string
                                             (assoc (g/generate (:put (:change-username login/schema)))
                                                    :new-username new-username))
                                      :content-type :json})
;            body (parse-body http-response)
            ]

        (is (= 200 (-> http-response :status)))
        (is (= "" (-> http-response :body bs/to-string)))
        (let [user-by-email (first (p/find (-> *system* :user-store) {:emailAddress new-username}))]
          (is (=  (-> *user-account-data*
                      (dissoc  :_id :password :birthDay :birthMonth)
                      (assoc  :emailAddress new-username :verifiedEmail false ))
                  (dissoc user-by-email :_id :password :birthMonth :birthDay))))))))




(deftest change-pw

  (testing ::login/change-password
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
            ])


      (is (-> *system* :mailer :mails deref first :content))

      (let [path (get-path ::login/change-password)
            data (g/generate (:put (:change-password login/schema)))
            http-response @(http/put (format "http://localhost:%s%s?access_token=%s"  port path (-> *system* :mailer :mails deref first :content))
                                     {:throw-exceptions false
                                      :body-encoding "UTF-8"
                                      :body (json/generate-string data)
                                      :content-type :json})
            body (parse-body http-response)]
        ;; checking that password for user is the new but encrypted :)
        (let [user-by-email (first (p/find (-> *system* :user-store) {:emailAddress (:emailAddress *user-account-data*)}))]
          (is (=  (p/check (:crypto *system*)
                           (:new-password data)
                           (:password user-by-email))))
          ;(is (= nil user-by-email))
          )

)



 )))

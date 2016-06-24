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
   [rebujito.base-test :refer (system-fixture *system* *user-account-data* *user-access-token* get-path  access-token-application access-token-user new-account-sb create-account new-sig  api-config)]
   [aleph.http :as http]
   [rebujito.api.resources
    [payment :as payment]
    [addresses :as addresses]
    [card :as card]
    [account :as account]
    [oauth :as oauth]
    [login :as login]]
   [clojure.test :refer :all]))


(use-fixtures :each (system-fixture #{:+mock-mimi :+ephemeral-db}))

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

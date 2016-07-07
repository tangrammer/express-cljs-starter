(ns rebujito.api.resources.profile-test
  (:require
   [rebujito.config :refer (config)]
   [schema.core :as s ]
   [bidi.bidi :as bidi]
   [byte-streams :as bs]
   [rebujito.protocols :as p]
   [taoensso.timbre :as log]
   [rebujito.api-test :refer (print-body parse-body)]
   [rebujito.api.resources.account :as account]
   [rebujito.api.resources.card :as card]
   [rebujito.api.resources.customer-admin :as customer-admin]
   [rebujito.api.resources.profile :as profile]
   [rebujito.api.resources.login :as login]
   [rebujito.base-test :refer (log-config system-fixture *user-account-data* *app-access-token* *system* *customer-admin-access-token* *user-access-token* get-path  access-token-application access-token-user new-account-sb create-account new-sig  api-config)]
   [aleph.http :as http]
   [clojure.pprint :refer (pprint)]
   [cheshire.core :as json]
   [clojure.test :refer :all]))


(use-fixtures :each (system-fixture #{:+mock-mimi :+ephemeral-db :+mock-mailer}))


(log/set-config! (log-config [["rebujito.*" :warn]
                              ["rebujito.security.*" :warn]
                              ["rebujito.mongo" :debug]
                              ["rebujito.mongo.*" :debug]
                              ["rebujito.api.*" :warn]
                              ["rebujito.api.util" :warn]]))

(deftest customer-admin-search
  (testing ::customer-admin/profile
    (let [r (-> *system* :docsite-router :routes)
          port (-> *system*  :webserver :port)
          api-id ::customer-admin/search
;          user-id (:user-id (p/read-token (:authenticator *system*) *user-access-token*))
          path (bidi/path-for r api-id)

          ]


      ;; forbidden with no valid user logged (user-access-token instead of customer-admin-access-token)
      (let [f-n ""
            l-n ""
            e-m ""
            c-n ""
            query (format  "&firstname=%s&surname=%s&email=%s&cardnumber=%s" f-n l-n e-m c-n)]
          (is (= 403 (-> @(http/get (format "http://localhost:%s%s?access_token=%s%s"  port path *user-access-token* query)
                                 {:throw-exceptions false
                                  :body-encoding "UTF-8"
                                  :content-type :json})
                      print-body
                      :status))))
      ;; 200 with  valid user logged
      (let [f-n ""
            l-n ""
            e-m ""
            c-n ""
            query (format  "&firstname=%s&surname=%s&email=%s&cardnumber=%s" f-n l-n e-m c-n)
            res @(http/get (format "http://localhost:%s%s?access_token=%s%s"  port path *customer-admin-access-token* query)
                                                                                                            {:throw-exceptions false
                                                                                                             :body-encoding "UTF-8"
                                                                                                             :content-type :json})
            body (parse-body res)
            ]
        (is (= 200 (-> res :status)))
                                        ;        (is (s/validate customer-admin/PagingSchema (:paging
        (s/validate customer-admin/PagingSchema (:paging body))
        (s/validate [customer-admin/SearchSchema] (:customers body))
        (is (pos?  (count (:customers body))))
        (pprint body)

        )

      ;; here adding a card to search by cardNumber
      (let [path (get-path ::card/register-digital-cards)]
        (is (= 200 (-> @(http/post (format "http://localhost:%s%s?access_token=%s"  port path *user-access-token*)
                                   {:throw-exceptions false
                                    :body-encoding "UTF-8"
                                    :content-type :json})
                       :status))))

      (let [user-to-find (p/find (:user-store *system*)
                                                (:user-id (p/read-token (:authenticator *system*) *user-access-token*)))
            f-n ""
            l-n ""
            e-m "" #_(apply str (take 3 (drop 3 (:emailAddress *user-account-data*) )))
            c-n (apply str (take 3 (->  user-to-find
                                        :cards first :cardNumber)))
            query (format  "&firstname=%s&surname=%s&email=%s&cardnumber=%s" f-n l-n e-m c-n)

            res @(http/get (format "http://localhost:%s%s?access_token=%s%s"  port path *customer-admin-access-token* query)
                                {:throw-exceptions false
                                 :body-encoding "UTF-8"
                                 :content-type :json})
            body (parse-body res)
            ]
        (is (= 200 (-> res :status)))
        (s/validate customer-admin/PagingSchema (:paging body))
        (s/validate [customer-admin/SearchSchema] (:customers body))
        (is (= 1  (count (:customers body))))
        (is (= (:emailAddress user-to-find)  (:emailAddress(first (:customers body)))))
        ))))

(deftest customer-admin-profile
  (testing ::customer-admin/profile
    (let [r (-> *system* :docsite-router :routes)
          port (-> *system*  :webserver :port)
          api-id ::customer-admin/profile
          user-id (:user-id (p/read-token (:authenticator *system*) *user-access-token*))
          path (bidi/path-for r api-id :user-id user-id)]


      ;; forbidden with no valid user logged (user-access-token instead of customer-admin-access-token)
      (is (= 403 (-> @(http/get (format "http://localhost:%s%s?access_token=%s"  port path *user-access-token*)
                                {:throw-exceptions false
                                 :body-encoding "UTF-8"
                                 :content-type :json})
                     print-body
                     :status)))
      ;; 200 with  valid user logged
      (is (= 200 (-> @(http/get (format "http://localhost:%s%s?access_token=%s"  port path *customer-admin-access-token*)
                                {:throw-exceptions false
                                 :body-encoding "UTF-8"
                                 :content-type :json})
                     print-body
                     :status)))



      ))
  )

(deftest me-profile
  (testing ::profile/me
    (let [r (-> *system* :docsite-router :routes)
          port (-> *system*  :webserver :port)
          api-id ::profile/me
          path (bidi/path-for r api-id)]
      (is (= 200 (-> @(http/get (format "http://localhost:%s%s?access_token=%s"  port path *user-access-token*)
                                {:throw-exceptions false
                                 :body-encoding "UTF-8"
                                 :content-type :json})
                     print-body
                     :status))))))

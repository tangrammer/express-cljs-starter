(ns rebujito.api.resources.profile-test
  (:require
   [bidi.bidi :as bidi]
   [byte-streams :as bs]
   [rebujito.protocols :as p]
   [rebujito.api-test :refer (print-body parse-body)]
   [rebujito.api.resources.account :as account]
   [rebujito.api.resources.customer-admin :as customer-admin]
   [rebujito.api.resources.profile :as profile]
   [rebujito.api.resources.login :as login]
   [rebujito.base-test :refer (system-fixture *app-access-token* *system* *customer-admin-access-token* *user-access-token* get-path  access-token-application access-token-user new-account-sb create-account new-sig  api-config)]
   [aleph.http :as http]
   [clojure.pprint :refer (pprint)]
   [cheshire.core :as json]
   [clojure.test :refer :all]))


(use-fixtures :each (system-fixture #{:+mock-mimi :+ephemeral-db :+mock-mailer}))



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

(ns rebujito.api.resources.account-test
  (:require
   [rebujito.api-test :refer (print-body)]
   [rebujito.api.resources.account :as account]
   [rebujito.base-test :refer (system-fixture *app-access-token* *system* *user-access-token* get-path  access-token-application access-token-user new-account-sb create-account new-sig  api-config)]
   [aleph.http :as http]
   [cheshire.core :as json]
   [clojure.test :refer :all]
   ))

(use-fixtures :each (system-fixture #{:+mock-mimi :+ephemeral-db}))

(deftest test-create-account
  (testing ::account/create
    (let [port (-> *system*  :webserver :port)
          path (get-path ::account/create)]
          (is (= 201  (-> @(http/post (format "http://localhost:%s%s?access_token=%s&market=%s"  port path *app-access-token* 1234)
                                      {:throw-exceptions false
                                       :body (json/generate-string
                                              (assoc (new-account-sb)
                                                     ;; string or int should both work here
                                                     :birthDay "1"
                                                     :birthMonth 1
                                                     ))
                                       :body-encoding "UTF-8"
                                       :content-type :json})
                          ;;                         print-body
                          :status)))))

  (testing "create-account-only"
    (create-account  (assoc (new-account-sb)
                            :birthDay "1"
                            :birthMonth "1"))))

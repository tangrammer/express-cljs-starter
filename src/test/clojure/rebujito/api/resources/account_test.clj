(ns rebujito.api.resources.account-test
  (:require
   [rebujito.protocols :as p]
   [rebujito.api-test :refer (print-body)]
   [rebujito.api.resources.account :as account]
   [rebujito.base-test :refer (system-fixture *app-access-token* *system* *user-access-token* get-path  access-token-application access-token-user new-account-sb create-account new-sig  api-config)]
   [aleph.http :as http]
   [clojure.pprint :refer (pprint)]
   [cheshire.core :as json]
   [clojure.test :refer :all]
   ))

(use-fixtures :each (system-fixture #{:+mock-mimi :+ephemeral-db}))

(deftest test-create-account
  (testing ::account/create
    (let [port (-> *system*  :webserver :port)
          path (get-path ::account/create)
          account-data (assoc (new-account-sb)
                              ;; string or int should both work here
                              :birthDay "1"
                              :birthMonth 1)
          users (seq (p/find (-> *system*  :user-store)))
          ]
      (pprint (first users))
          (is (= 201  (-> @(http/post (format "http://localhost:%s%s?access_token=%s&market=%s"  port path *app-access-token* 1234)
                                      {:throw-exceptions false
                                       :body (json/generate-string account-data)
                                       :body-encoding "UTF-8"
                                       :content-type :json})
                          ;;                         print-body
                          :status)))
          (pprint (first (seq (p/find (-> *system*  :user-store)))))
          (is (= (count (seq (p/find (-> *system*  :user-store)))) (inc (count users))))
          ;; same account throws error
          (is (= 400  (-> @(http/post (format "http://localhost:%s%s?access_token=%s&market=%s"  port path *app-access-token* 1234)
                                      {:throw-exceptions false
                                       :body (json/generate-string account-data)
                                       :body-encoding "UTF-8"
                                       :content-type :json})
                          ;;                         print-body
                          :status)))
          (is (= (count (seq (p/find (-> *system*  :user-store)))) (inc (count users))))
          ))

  (testing "create-account-only"
    (create-account  (assoc (new-account-sb)
                            :birthDay "1"
                            :birthMonth "1"))


    ))

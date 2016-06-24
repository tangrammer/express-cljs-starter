(ns rebujito.api.resources.account-test
  (:require
   [byte-streams :as bs]
   [rebujito.protocols :as p]
   [rebujito.api-test :refer (print-body parse-body)]
   [rebujito.api.resources.account :as account]
   [rebujito.base-test :refer (system-fixture *app-access-token* *system* *user-access-token* get-path  access-token-application access-token-user new-account-sb create-account new-sig  api-config)]
   [aleph.http :as http]
   [clojure.pprint :refer (pprint)]
   [cheshire.core :as json]
   [clojure.test :refer :all]))

(use-fixtures :each (system-fixture #{:+mock-mimi :+ephemeral-db}))

(deftest test-create-account
  (testing ::account/create
    (let [port (-> *system*  :webserver :port)
          path (get-path ::account/create)
          account-data (assoc (new-account-sb)
                              ;; string or int should both work here
                              :birthDay "1"
                              :birthMonth 1)
          users (seq (p/find (-> *system*  :user-store)))]

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
      (let [res @(http/post (format "http://localhost:%s%s?access_token=%s&market=%s"  port path *app-access-token* 1234)
                            {:throw-exceptions false
                             :body (json/generate-string account-data)
                             :body-encoding "UTF-8"
                             :content-type :json})]

        (is (= 400  (-> res :status)))
        (is (= {:code 111027,
                :body "Account Management Service returns error that email address is already taken"}
               (select-keys (-> res parse-body) [:code :body]))))

      (is (= (count (seq (p/find (-> *system*  :user-store)))) (inc (count users))))))

  (testing "create-account-only"
    (create-account  (assoc (new-account-sb)
                            :birthDay "1"
                            :birthMonth "1"))))

(deftest test-market-parameter-preference
  (testing ::account/create-market-parameter-preference
    (let [port (-> *system* :webserver :port)
          query-market "ZA"
          body-market "FR"]

      (let [account-data (assoc (new-account-sb) :market body-market)

            http-response @(http/post (format "http://localhost:%s%s?access_token=%s&market=%s" port (get-path ::account/create) *app-access-token* query-market)
                                      {:throw-exceptions false
                                       :body (json/generate-string account-data)
                                       :body-encoding "UTF-8"
                                       :content-type :json})
            body (parse-body http-response)]

        (is (= 201  (->  http-response :status)))
        (is (= body-market  (:market body)))
        (is (= body-market (:market (p/find (-> *system*  :user-store) (:_id body))))))

      (let [account-data (dissoc (new-account-sb))

            http-response @(http/post (format "http://localhost:%s%s?access_token=%s&market=%s" port (get-path ::account/create) *app-access-token* query-market)
                                      {:throw-exceptions false
                                       :body (json/generate-string account-data)
                                       :body-encoding "UTF-8"
                                       :content-type :json})
            body (parse-body http-response)]

        (is (= 201  (->  http-response :status)))
        (is (= query-market  (:market body)))
        (is (= query-market (:market (p/find (-> *system*  :user-store) (:_id body)))))))))

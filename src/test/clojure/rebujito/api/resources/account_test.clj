(ns rebujito.api.resources.account-test
  (:require
   [byte-streams :as bs]
   [rebujito.protocols :as p]
   [rebujito.api-test :refer (print-body parse-body)]
   [rebujito.api.resources.account :as account]
   [rebujito.api.resources.login :as login]
   [rebujito.base-test :refer (system-fixture *app-access-token* *system* *user-access-token* get-path  access-token-application access-token-user new-account-sb create-account new-sig  api-config)]
   [aleph.http :as http]
   [clojure.pprint :refer (pprint)]
   [cheshire.core :as json]
   [clojure.test :refer :all]))

(use-fixtures :each (system-fixture #{:+mock-mimi :+ephemeral-db :+mock-mailer}))

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

      (is (= 2 (count (deref(:mails (-> *system*  :mailer))))))
      (is (= 201  (-> @(http/post (format "http://localhost:%s%s?access_token=%s&market=%s"  port path *app-access-token* 1234)
                                  {:throw-exceptions false
                                   :body (json/generate-string account-data)
                                   :body-encoding "UTF-8"
                                   :content-type :json})
                          ;;                         print-body
                      :status)))
      (pprint (first (seq (p/find (-> *system*  :user-store)))))

      (is (= 3 (count (deref(:mails (-> *system*  :mailer))))))

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
      ;; doesn't mail again thus an error happen
      (is (= 3 (count (deref(:mails (-> *system*  :mailer))))))
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



(deftest verify-account
(testing ::account/create
    (let [port (-> *system*  :webserver :port)
          path (get-path ::account/create)
          account-data (assoc (new-account-sb)
                              ;; string or int should both work here
                              :birthDay "1"
                              :birthMonth 1)
          users (seq (p/find (-> *system*  :user-store)))]



      (is (= 2 (count (deref(:mails (-> *system*  :mailer))))))

      (let [res @(http/post (format "http://localhost:%s%s?access_token=%s&market=%s"  port path *app-access-token* 1234)
                                  {:throw-exceptions false
                                   :body (json/generate-string account-data)
                                   :body-encoding "UTF-8"
                                   :content-type :json})]
        (is (= 201  (-> res :status)))
        (is (= false  (:verifiedEmail (parse-body res) :doesnt-exist!)))
        (is (= (count (seq (p/find (-> *system*  :user-store)))) (inc (count users))))
        )

      (let [mails (deref(:mails (-> *system*  :mailer)))
            verify-email (last mails)
            token (last (clojure.string/split
                                   (:hidden verify-email) #"/")) ]
        (is (= 3 (count mails)))
        (is (= {:subject "Verify your Starbucks Rewards email"
                :to (:emailAddress account-data)}
               (select-keys verify-email [:subject :to])))


        (let [path (get-path ::login/verify-email)
              res-verify @(http/put (format "http://localhost:%s%s?access_token=%s"  port path token)
                                  {:throw-exceptions false
                                   :body-encoding "UTF-8"
                                   :content-type :json})]

          (is (= 200 (:status res-verify)))
          (is (= true (:verifiedEmail (first (p/find (-> *system*  :user-store ) {:emailAddress (:emailAddress account-data)}))) ))
          )


        (let [path (get-path ::login/verify-email)
              res-verify @(http/put (format "http://localhost:%s%s?access_token=%s"  port path token)
                                  {:throw-exceptions false
                                   :body-encoding "UTF-8"
                                   :content-type :json})]

          (is (= 401 (:status res-verify)))

          )



        )
))

  )

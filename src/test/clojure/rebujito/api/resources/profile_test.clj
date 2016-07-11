(ns rebujito.api.resources.profile-test
  (:require
   [rebujito.config :refer (config)]
   [schema.core :as s ]
   [bidi.bidi :as bidi]
   [byte-streams :as bs]
   [schema-generators.generators :as g]
   [rebujito.protocols :as p]
   [taoensso.timbre :as log]
   [rebujito.api-test :refer (print-body parse-body create-digital-card*)]
   [rebujito.api.resources.account :as account]
   [rebujito.api.util :as api-util]
   [rebujito.api.resources.card :as card]
   [rebujito.api.resources.payment :as payment]
   [rebujito.api.resources.addresses :as addresses]
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
                              ["rebujito.mongo" :info]
                              ["rebujito.mimi" :info]
                              ["rebujito.mimi.*" :info]
                              ["rebujito.mongo.*" :info]
                              ["rebujito.api.*" :info]
                              ["rebujito.api.util" :warn]]))


(deftest customer-user-update
(log/set-config! (log-config [["rebujito.*" :warn]
                              ["rebujito.security.*" :warn]
                              ["rebujito.mongo" :info]
                              ["rebujito.mimi" :info]
                              ["rebujito.mimi.*" :info]
                              ["rebujito.mongo.*" :info]
                              ["rebujito.api.*" :info]
                              ["rebujito.api.util" :warn]]))
  (testing ::customer-admin/user
    (let [r (-> *system* :docsite-router :routes)
          port (-> *system*  :webserver :port)]

      (let [api-id ::customer-admin/user
            user-id (:user-id (p/read-token (:authenticator *system*) *user-access-token*))
            path (bidi/path-for r api-id :user-id user-id)
            payload (g/generate (:put (:user customer-admin/schema)))
            ]

        ;; forbidden with no valid user logged (user-access-token instead of customer-admin-access-token)
        (is (= 403 (-> @(http/put (format "http://localhost:%s%s?access_token=%s"  port path *user-access-token*)
                                  {:throw-exceptions false
                                   :body-encoding "UTF-8"
                                   :body (json/generate-string payload)
                                   :content-type :json})
                       :status)))

        ;; 500 with  existent email
        (let [res @(http/put (format "http://localhost:%s%s?access_token=%s"  port path *customer-admin-access-token*)
                             {:throw-exceptions false
                              :body-encoding "UTF-8"
                              :body (json/generate-string (assoc  payload :emailAddress (:emailAddress *user-account-data*)))
                              :content-type :json})
              body (parse-body res false)]
          (is (= 200 (-> res :status))))

        ;; 200 with  valid user logged
        (let [res @(http/put (format "http://localhost:%s%s?access_token=%s"  port path *customer-admin-access-token*)
                             {:throw-exceptions false
                              :body-encoding "UTF-8"
                              :body (json/generate-string payload)
                              :content-type :json})
              body (parse-body res false)]
          (is (= 200 (-> res :status)))
          (is (=  "" body))
          (is (= (api-util/remove-nils payload) (select-keys (p/find (:user-store *system*) user-id) (keys (api-util/remove-nils payload)))))
          )


        ))))

(deftest customer-forgot-password

  (testing ::customer-admin/forgot-password
    (let [r (-> *system* :docsite-router :routes)
          port (-> *system*  :webserver :port)]

      (let [api-id ::customer-admin/forgot-password
            user-id (:user-id (p/read-token (:authenticator *system*) *user-access-token*))
            path (bidi/path-for r api-id :user-id user-id)]

        ;; forbidden with no valid user logged (user-access-token instead of customer-admin-access-token)
        (is (= 403 (-> @(http/post (format "http://localhost:%s%s?access_token=%s"  port path *user-access-token*)
                                  {:throw-exceptions false
                                   :body-encoding "UTF-8"
                                   :content-type :json})
                       :status)))


        ;; 200 with  valid user logged
        (let [res @(http/post (format "http://localhost:%s%s?access_token=%s"  port path *customer-admin-access-token*)
                             {:throw-exceptions false
                              :body-encoding "UTF-8"
                              :content-type :json})
              body (parse-body res)]
          (is (= 200 (-> res :status)))
          (is (=  nil body))
          (let [mails @(:mails (:mailer *system*))]
          (is (= 3 (count mails)))

          (is (.contains (:subject (last mails)) "Reset your Starbucks Rewards Password")  )
          (is (= user-id (:user-id (p/read-token (:authenticator *system*)
                                    (last (clojure.string/split
                                           (-> mails first :hidden) #"/"))))))))))))


(deftest customer-admin-update-address
  (testing ::customer-admin/address
    (let [r (-> *system* :docsite-router :routes)
          port (-> *system*  :webserver :port)
          address-id (atom "")]
      ;; create!
      (let [path (get-path ::addresses/addresses)
            http-res @(http/post (format "http://localhost:%s%s?access_token=%s"  port path *user-access-token*)
                                 {:throw-exceptions false
                                  :body-encoding "UTF-8"
                                  :body (json/generate-string
                                         (g/generate (:post addresses/schema))

                                         )
                                  :content-type :json})
            _ (is (= (:status http-res) 201))
            body (parse-body http-res)]

        ;; checking the header "/me/addresses/a8963052-9131-475d-9a23-40a3d2f109cc"
        (is  (:location (clojure.walk/keywordize-keys (-> http-res :headers ))))
        (reset! address-id (last (clojure.string/split (:location (clojure.walk/keywordize-keys (-> http-res :headers ))) #"\/"))))

      (let [api-id ::customer-admin/address
            user-id (:user-id (p/read-token (:authenticator *system*) *user-access-token*))
            path (bidi/path-for r api-id :user-id user-id :address-id @address-id)
            update-payload (g/generate (:put addresses/schema))
            ]

        ;; forbidden with no valid user logged (user-access-token instead of customer-admin-access-token)
        (is (= 403 (-> @(http/put (format "http://localhost:%s%s?access_token=%s"  port path *user-access-token*)
                                  {:throw-exceptions false
                                   :body-encoding "UTF-8"
                                   :body (json/generate-string update-payload)
                                   :content-type :json})
                       :status)))


        ;; 200 with  valid user logged
        (let [user-to-find (p/find (:user-store *system*)
                                   (:user-id (p/read-token (:authenticator *system*) *user-access-token*)))
              res @(http/put (format "http://localhost:%s%s?access_token=%s"  port path *customer-admin-access-token*)
                             {:throw-exceptions false
                              :body-encoding "UTF-8"
                              :body (json/generate-string  update-payload)
                              :content-type :json})
              body (parse-body res)]
          (is (= 200 (-> res :status)))
          (is (=  nil body))
          )


        ;; get-detail
        (let [path (bidi/path-for (-> *system* :docsite-router :routes) ::addresses/get :address-id @address-id)
              http-response @(http/get (format "http://localhost:%s%s?access_token=%s"  port path *user-access-token*)
                                       {:throw-exceptions false
                                        :body-encoding "UTF-8"
                                        :content-type :json})
              body (parse-body http-response)
              ]
          (is (= 200 (-> http-response :status)))
          (is (= (select-keys update-payload [:addressLine1 :name :city ]) (select-keys body [:addressLine1 :name :city ]))))


        ))
    ))

(deftest customer-admin-history
  (testing ::customer-admin/history
    (let [r (-> *system* :docsite-router :routes)
          port (-> *system*  :webserver :port)
          api-id ::customer-admin/history
          user-id (:user-id (p/read-token (:authenticator *system*) *user-access-token*))
          path (bidi/path-for r api-id :user-id user-id)]

      ;; forbidden with no valid user logged (user-access-token instead of customer-admin-access-token)
      (is (= 403 (-> @(http/get (format "http://localhost:%s%s?access_token=%s"  port path *user-access-token*)
                                {:throw-exceptions false
                                 :body-encoding "UTF-8"
                                 :content-type :json})
                     :status)))

      ;; here adding a card to search by cardNumber
      (let [path (get-path ::card/register-digital-cards)]
        (is (= 200 (-> @(http/post (format "http://localhost:%s%s?access_token=%s"  port path *user-access-token*)
                                   {:throw-exceptions false
                                    :body-encoding "UTF-8"
                                    :content-type :json})
                       :status))))

      ;; 200 with  valid user logged
      (let [user-to-find (p/find (:user-store *system*)
                                 (:user-id (p/read-token (:authenticator *system*) *user-access-token*)))
            card-number (->  user-to-find :cards first :cardNumber)
            res @(http/get (format "http://localhost:%s%s?access_token=%s"  port path *customer-admin-access-token*)
                                {:throw-exceptions false
                                 :body-encoding "UTF-8"
                                 :content-type :json})]
        (is (= 200 (-> res :status)))
        (is (= (#'rebujito.api.resources.card/filter-history (rebujito.mimi.mocks/get-history)) (:historyItems (parse-body res))))
        ))
    )
  )

(deftest customer-admin-transfer-from
  (testing ::customer-admin/transfer-from
    (let [r (-> *system* :docsite-router :routes)
          port (-> *system*  :webserver :port)
          api-id ::customer-admin/transfer-from
          user-id (:user-id (p/read-token (:authenticator *system*) *user-access-token*))
          path (bidi/path-for r api-id :user-id user-id)]

      ;; forbidden with no valid user logged (user-access-token instead of customer-admin-access-token)
      (is (= 403 (-> @(http/post (format "http://localhost:%s%s?access_token=%s"  port path *user-access-token*)
                                {:throw-exceptions false
                                 :body-encoding "UTF-8"
                                 :content-type :json})
                     print-body
                     :status)))

      ;; here adding a card to search by cardNumber
      (let [path (get-path ::card/register-digital-cards)]
        (is (= 200 (-> @(http/post (format "http://localhost:%s%s?access_token=%s"  port path *user-access-token*)
                                   {:throw-exceptions false
                                    :body-encoding "UTF-8"
                                    :content-type :json})
                       :status))))

      ;; 200 with  valid user logged
      (let [user-to-find (p/find (:user-store *system*)
                                 (:user-id (p/read-token (:authenticator *system*) *user-access-token*)))
            card-number (->  user-to-find :cards first :cardNumber)
            res @(http/post (format "http://localhost:%s%s?access_token=%s"  port path *customer-admin-access-token*)
                                {:throw-exceptions false
                                 :body-encoding "UTF-8"
                                 :body (json/generate-string
                                                {:cardNumber card-number
                                                 :cardPin "???"})
                                 :content-type :json})]
        (is (= 200 (-> res :status)))
;        (is (= nil (parse-body res)))
        ))
    )
  )

(deftest customer-admin-transfer-to
  (testing ::customer-admin/transfer-to
    (let [r (-> *system* :docsite-router :routes)
          port (-> *system*  :webserver :port)
          api-id ::customer-admin/transfer-to
          user-id (:user-id (p/read-token (:authenticator *system*) *user-access-token*))
          path (bidi/path-for r api-id :user-id user-id)]

      ;; forbidden with no valid user logged (user-access-token instead of customer-admin-access-token)
      (is (= 403 (-> @(http/post (format "http://localhost:%s%s?access_token=%s"  port path *user-access-token*)
                                {:throw-exceptions false
                                 :body-encoding "UTF-8"
                                 :content-type :json})
                     print-body
                     :status)))

      ;; here adding a card to search by cardNumber
      (let [path (get-path ::card/register-digital-cards)]
        (is (= 200 (-> @(http/post (format "http://localhost:%s%s?access_token=%s"  port path *user-access-token*)
                                   {:throw-exceptions false
                                    :body-encoding "UTF-8"
                                    :content-type :json})
                       :status))))

      ;; 200 with  valid user logged
      (let [user-to-find (p/find (:user-store *system*)
                                 (:user-id (p/read-token (:authenticator *system*) *user-access-token*)))
            card-number (->  user-to-find :cards first :cardNumber)
            res @(http/post (format "http://localhost:%s%s?access_token=%s"  port path *customer-admin-access-token*)
                                {:throw-exceptions false
                                 :body-encoding "UTF-8"
                                 :body (json/generate-string
                                                {:cardNumber card-number
                                                 :cardPin "???"})
                                 :content-type :json})]
        (is (= 200 (-> res :status)))
;        (is (= nil (parse-body res)))
        ))
    )
  )

#_(deftest customer-admin-transfer-to-new-digital
  (testing ::customer-admin/transfer-to-new-digital
    (let [r (-> *system* :docsite-router :routes)
          port (-> *system*  :webserver :port)
          api-id ::customer-admin/transfer-to-new-digital
          user-id (:user-id (p/read-token (:authenticator *system*) *user-access-token*))
          path (bidi/path-for r api-id :user-id user-id)]

      ;; forbidden with no valid user logged (user-access-token instead of customer-admin-access-token)
      (is (= 403 (-> @(http/post (format "http://localhost:%s%s?access_token=%s"  port path *user-access-token*)
                                {:throw-exceptions false
                                 :body-encoding "UTF-8"
                                 :content-type :json})
                     print-body
                     :status)))


      (let [user-to-find (p/find (:user-store *system*)
                                 (:user-id (p/read-token (:authenticator *system*) *user-access-token*)))
            card-number (->  user-to-find :cards first :cardNumber)
            res @(http/post (format "http://localhost:%s%s?access_token=%s"  port path *customer-admin-access-token*)
                                {:throw-exceptions false
                                 :body-encoding "UTF-8"

                                 :content-type :json})]
        (is (= 200 (-> res :status)))
        (is (= {:status "ok"} (parse-body res)))
        ))
    )
  )

(deftest customer-admin-add-stars
  (testing ::customer-admin/profile
    (let [r (-> *system* :docsite-router :routes)
          port (-> *system*  :webserver :port)

          ]

      ;; here adding a card to search by cardNumber
      (let [path (get-path ::card/register-digital-cards)]
        (is (= 200 (-> @(http/post (format "http://localhost:%s%s?access_token=%s"  port path *user-access-token*)
                                   {:throw-exceptions false
                                    :body-encoding "UTF-8"
                                    :content-type :json})
                       :status))))

      (let [tok (p/read-token (:authenticator *system*) *user-access-token*)
            user-id (:user-id tok)
            api-id ::customer-admin/add-stars
            path (bidi/path-for r api-id :user-id user-id)
            res @(http/put (format "http://localhost:%s%s?access_token=%s"  port path *customer-admin-access-token*)
                           {:throw-exceptions false
                            :body (json/generate-string {:amount 149})
                            :body-encoding "UTF-8"
                            :content-type :json})
            body (parse-body res)
            ]
        (is (= 200 (-> res :status)))

        (pprint body)
        ))))

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
      ;; checking limit and offset too
      (let [f-n ""
            l-n ""
            e-m ""
            c-n ""
            limit 1
            offset 0
            query (format  "&firstname=%s&surname=%s&email=%s&cardnumber=%s&limit=%s&offset=%s" f-n l-n e-m c-n limit offset)

            query2 (format  "&firstname=%s&surname=%s&email=%s&cardnumber=%s&limit=%s&offset=%s" f-n l-n e-m c-n limit (inc offset))
            res @(http/get (format "http://localhost:%s%s?access_token=%s%s"  port path *customer-admin-access-token* query)
                                                                                                            {:throw-exceptions false
                                                                                                             :body-encoding "UTF-8"
                                                                                                             :content-type :json})
            res2 @(http/get (format "http://localhost:%s%s?access_token=%s%s"  port path *customer-admin-access-token* query2)
                                                                                                            {:throw-exceptions false
                                                                                                             :body-encoding "UTF-8"
                                                                                                             :content-type :json})
            body (parse-body res)
            body2 (parse-body res2)
            ]
        (is (= 200 (-> res :status)))
                                        ;        (is (s/validate customer-admin/PagingSchema (:paging
        (s/validate customer-admin/PagingSchema (:paging body))
        (s/validate [customer-admin/SearchSchema] (:customers body))
        (is (pos?  (count (:customers body))))
        (is (pos?  (count (:customers body2))))
        (pprint  body)
        (pprint  body2)

        )

      ;; here adding a card to search by cardNumber
      (let [path (get-path ::card/register-digital-cards)]
        (is (= 200 (-> @(http/post (format "http://localhost:%s%s?access_token=%s"  port path *user-access-token*)
                                   {:throw-exceptions false
                                    :body-encoding "UTF-8"
                                    :content-type :json})
                       :status))))

      (let [tok (p/read-token (:authenticator *system*) *user-access-token*)
            _ (println "TOK" tok)
            user-to-find (p/find (:user-store *system*)
                                                (:user-id tok))
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
        (pprint body)
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
          path (bidi/path-for r api-id)
          res @(http/get (format "http://localhost:%s%s?access_token=%s"  port path *user-access-token*)
                                {:throw-exceptions false
                                 :body-encoding "UTF-8"
                                 :content-type :json})
          body (parse-body res)]
      (is (= 200 (-> res :status)))
      (is (-> body :user :createdDate))
      )))

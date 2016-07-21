(ns rebujito.api.resources.card-test
  (:require
   [bidi.bidi :as bidi]
   [byte-streams :as bs]
   [rebujito.protocols :as p]
   [schema-generators.generators :as g]
   [rebujito.api.util :as util]
   [rebujito.api-test :refer (print-body parse-body create-digital-card*)]
   [rebujito.api.resources.account :as account]
   [rebujito.api.resources.card :as card]
   [rebujito.base-test :refer (system-fixture *app-access-token* *system* *user-access-token* get-path  access-token-application access-token-user new-account-sb create-account new-sig  api-config)]
   [aleph.http :as http]
   [clojure.pprint :refer (pprint)]
   [cheshire.core :as json]
   [clojure.test :refer :all]
   [rebujito.api.resources.card
    [reload :as card-reload]]
   ))

(use-fixtures :each (system-fixture #{:+mock-mimi :+ephemeral-db :+mock-mailer :+mock-resource-pool}))

(deftest test-cards
  (let [port (-> *system*  :webserver :port)
        r (-> *system* :docsite-router :routes)
        ]

    (testing ::card/history
      (let [path (get-path ::card/history)
            {:keys [status body]}
            (-> @(http/get (format "http://localhost:%s%s?access_token=%s"  port path *user-access-token*)
                                        {:throw-exceptions false
                                         :body-encoding "UTF-8"
                                         :content-type :json}))
            _ (is (= status 200))
            body (-> (bs/to-string body)
                     (json/parse-string true))]

        (clojure.pprint/pprint body)
        (is (= {:paging {:total 0, :returned 0, :offset 0, :limit 50},
                :historyItems []} body))))

    (testing ::card/get-cards2
      (let [path (get-path ::card/get-cards)
            {:keys [status body]}
            (-> @(http/get (format "http://localhost:%s%s?access_token=%s"  port path *user-access-token*)
                                        {:throw-exceptions false
                                         :body-encoding "UTF-8"
                                         :content-type :json}))
            _ (is (= status 200))
            body (-> (bs/to-string body)
                     (json/parse-string true))]

        (clojure.pprint/pprint body)
        (is (= [] body))))

    (testing ::card/register-physical
      (let [path (get-path ::card/register-physical)]
                                        ;         (println (format "http://localhost:%s%s?access_token=%s"  port path 123))
        (is (= 200 (-> @(http/post (format "http://localhost:%s%s?access_token=%s"  port path *user-access-token*)
                                   {:throw-exceptions false
                                    :body-encoding "UTF-8"
                                    :body (json/generate-string
                                           (assoc (merge (g/generate (-> card/schema :register-physical :post))
                                                         (g/generate util/optional-risk))
                                                  :cardNumber (str (+ (rand-int 1000) (read-string (format "96235709%05d" 0)))))
                                           )
                                    :content-type :json})
                       ;;                        print-body
                       :status)))))

    (testing ::card/register-digital-cards
      (let [path (get-path ::card/register-digital-cards)]
        ;;         (println (format "http://localhost:%s%s?access_token=%s"  port path 123))
        (is (= 200 (-> @(http/post (format "http://localhost:%s%s?access_token=%s"  port path *user-access-token*)
                                   {:throw-exceptions false
                                    :body-encoding "UTF-8"
                                    :content-type :json})
                       :status)))))

    (testing ::card-reload/reload
      (let [api-id ::card-reload/reload
            path (bidi/path-for r api-id :card-id 123)]
        ;;         (println (format "http://localhost:%s%s?access_token=%s"  port path 123))
        (is (= 400(-> @(http/post (format "http://localhost:%s%s?access_token=%s"  port path *user-access-token*)
                                  {:throw-exceptions false
                                   :body-encoding "UTF-8"
                                   :body (json/generate-string
                                          {
                                           :amount 15
                                           :paymentMethodId "1234567"
                                           :sessionId ""
                                           })
                                   :content-type :json})
                      (print-body)
                      :status)))))

    (testing ::card/balance
      (let [api-id ::card/balance
            path (bidi/path-for r api-id :card-id 123)]
        ;;         (println (format "http://localhost:%s%s?access_token=%s"  port path 123))
        (is (= 200(-> @(http/get (format "http://localhost:%s%s?access_token=%s"  port path *user-access-token*)
                                  {:throw-exceptions false
                                   :body-encoding "UTF-8"
                                   :content-type :json})
                      (print-body)
                      :status)))))

    (testing ::card/balance-realtime
      (let [api-id ::card/balance-realtime
            path (bidi/path-for r api-id :card-id 123)]
        ;;         (println (format "http://localhost:%s%s?access_token=%s"  port path 123))
        (is (= 200(-> @(http/get (format "http://localhost:%s%s?access_token=%s"  port path *user-access-token*)
                                  {:throw-exceptions false
                                   :body-encoding "UTF-8"
                                   :content-type :json})
                      (print-body)
                      :status)))))

    ;; TODO: tried to do a test for transfer card, but not sure how to tackle it
    #_(testing ::card/transfer
      (let [api-id ::card/transfer
            path (bidi/path-for r api-id)
            {:keys [status body]}
            (-> @(http/post (format "http://localhost:%s%s?access_token=%s"  port path *user-access-token*)
                                        {:throw-exceptions false
                                         :body-encoding "UTF-8"
                                         :body (json/generate-string
                                                {:cardNumber "???"
                                                 :cardPin "???"})
                                         :content-type :json}))]
       (is (= 200 status))))
    )
)

#_(deftest test-transfer-legacy
  (let [port (-> *system*  :webserver :port)
        r (-> *system* :docsite-router :routes)
        user-id "00000000000000000007a51e"]

   (is (= 1 2))

    ; TODO
    ; add-card for user-id

    (testing "get-card-data"
      (is (= {}
             (card/get-card-data (-> *system* :user-store) user-id)))
    )

    (testing ::card/transfer-legacy
      (let [api-id ::card/transfer-legacy
            path (bidi/path-for r api-id)
            {:keys [status body]}
            (-> @(http/post (format "http://localhost:%s%s?access_token=%s"  port path *user-access-token*)
                                        {:throw-exceptions false
                                         :body-encoding "UTF-8"
                                         :body (json/generate-string
                                                {:sourceCardNumber "80008000"
                                                 :sourceCardPin "1234"
                                                 :targetCardNumber "80008000"
                                                 :goldCardActivation false})
                                         :content-type :json}))]
        (is (= 200 status))
        ))
))


(deftest transfer-to-new-digital
  (testing ::card/transfer-to-new-digital
    (let [
          port (-> *system*  :webserver :port)
          user-id (:user-id (p/read-token (:authenticator *system*) *user-access-token*))
          path (get-path ::card/transfer-to-new-digital)
          _ (create-digital-card*)
          ]

      ;; unauthenticated
      (is (= 401 (-> @(http/post (format "http://localhost:%s%s?access_token=%s" port path "invalid-token")
                                 {:throw-exceptions false
                                  :body-encoding "UTF-8"
                                  :content-type :json})
                     print-body
                     :status)))

      (let [original-user (p/find (:user-store *system*) user-id)
            old-card-number (-> original-user :cards first :cardNumber)
            res @(http/post (format "http://localhost:%s%s?access_token=%s" port path *user-access-token*)
                            {:throw-exceptions false
                             :body-encoding "UTF-8"
                             :content-type :json})]
        (is (= 200 (-> res :status)))
        (is (= {:status "ok"} (parse-body res)))
        (is (= 1 (-> original-user :cards count)))
        (let [new-user (p/find (:user-store *system*) user-id)
              new-card-number (-> new-user :cards first :cardNumber)]
          (is (not (= original-user new-user)))
          (is (not (= old-card-number new-card-number)))
          (is (= 1 (-> new-user :cards count)))
          )
        ))
    )
  )
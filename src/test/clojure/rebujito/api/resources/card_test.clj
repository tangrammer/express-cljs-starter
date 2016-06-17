(ns rebujito.api.resources.card-test
  (:require
   [bidi.bidi :as bidi]
   [byte-streams :as bs]
   [rebujito.protocols :as p]
   [schema-generators.generators :as g]
   [rebujito.api.util :as util]
   [rebujito.api-test :refer (print-body)]
   [rebujito.api.resources.account :as account]
   [rebujito.api.resources.card :as card]
   [rebujito.base-test :refer (system-fixture *app-access-token* *system* *user-access-token* get-path  access-token-application access-token-user new-account-sb create-account new-sig  api-config)]
   [aleph.http :as http]
   [clojure.pprint :refer (pprint)]
   [cheshire.core :as json]
   [clojure.test :refer :all]
   ))

(use-fixtures :each (system-fixture #{:+mock-mimi :+ephemeral-db}))

(deftest test-cards
  (let [port (-> *system*  :webserver :port)
        r (-> *system* :docsite-router :routes)
        ]

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

    (testing "card/unregister"
      (let [api-id ::card/card
            path (bidi/path-for r api-id :card-id 123)]
        (is (= 200 (-> @(http/delete (format "http://localhost:%s%s?access_token=%s"  port path 123)
                                     {:throw-exceptions false
                                      :body-encoding "UTF-8"
                                      :content-type :json})
                       :status)))))

    (testing ::card/reload
      (let [api-id ::card/reload
            path (bidi/path-for r api-id :card-id 123)]
        ;;         (println (format "http://localhost:%s%s?access_token=%s"  port path 123))
        (is (= 200(-> @(http/post (format "http://localhost:%s%s?access_token=%s"  port path *user-access-token*)
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

    )

)

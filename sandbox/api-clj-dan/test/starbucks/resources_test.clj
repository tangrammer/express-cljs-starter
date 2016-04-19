(ns starbucks.resources-test
  (:require
   [clojure.tools.logging :as log]
   [byte-streams :as b]
   [bidi.bidi :as bidi]
   [bidi.vhosts :refer [make-handler vhosts-model]]
   [clojure.java.io :as io]
   [clojure.set :as set]
   [clojure.test :refer :all]
   [clojure.edn :as edn]
   [clojure.data.codec.base64 :as base64]
   [ring.mock.request :refer [request] :as mock]
   [cheshire.core :as json]
   [starbucks.util :as utl]
   [starbucks.db :as db]
   [starbucks.api :refer [api]]
   ))

(def
  ^{:doc "only for tests"}
  test_full_seed {1 {:username "Malcolm Sparks"
                     :userid "mspar"
                     :password "1234"}
                  2 {:username "Jon Pither"
                     :userid "jpit"
                     :password "5678"}})

(defn create-api [db]
  (let [api (api db)]
    (vhosts-model [{:scheme :http :host "localhost"} api])))

(deftest list-all-entries
  (let [db (db/create-db test_full_seed)
        h (make-handler (create-api db))
        req (merge-with merge
                        (mock/request :get "/starbucks")
                        {:headers {"accept" "application/edn"}})
        response @(h req)]

    (is (= 200 (:status response)))

    (let [body (-> response :body utl/to-string edn/read-string)]
      (is (= 2 (count body)))
      (is (= "mspar" (get-in body [1 :userid]))))))

(deftest create-entry
  (let [db (db/create-db {})
        h (make-handler (create-api db))
        req (-> (mock/request :post "/starbucks" {"username" "Jon Pither" "userid" "jpit" "password" "4567"})
                (update :headers assoc
                        "authorization" (utl/encode-basic-authorization "john" "doe")))
        response @(h req)]

    (is (= 201 (:status response)))
    #_(is (set/superset? (set (keys (:headers response)))
                         #{"location" "content-length"}))
    #_(is (nil? (:body response)))))

(deftest update-entry
  (let [db (db/create-db test_full_seed)
        h (make-handler (create-api db))]
    (is (= (db/count-entries db) 2))
    (let [req (->
               (mock/request :put "/starbucks/2" (slurp (io/resource "starbucks/update-data")))
               (update :headers assoc
                       "content-type" "multipart/form-data; boundary=ABCD"
                       "authorization" (utl/encode-basic-authorization "john" "doe")))
          response @(h req)]

      (is (= 204 (:status response)))
      (is (nil? (:body response)))

      (is (= (db/count-entries db) 2))
      (is (= "8888" (:password (db/get-entry db 2)))))))

(deftest delete-entry
  (let [db (db/create-db test_full_seed)
        h (make-handler (create-api db))]
    (is (= (db/count-entries db) 2))
    (let [req (-> (mock/request :delete "/starbucks/1")
                  (update :headers assoc
                          "accept" "text/plain"
                          "authorization" (utl/encode-basic-authorization "john" "doe")))
          response @(h req)]
      (is (= 200 (:status response)))
      (is (= "Entry 1 has been removed\n" (b/to-string (:body response))))
      (is (= (db/count-entries db) 1)))))


(def
  ^{:doc "only for tests"}
  test_token_data {:client_id "dev"
                   :client_secret "abcd"
                   :code "1234"
                   :redirect_url "https://www.starbucks.com/"
                   :scope "test"
                   }
  )

(deftest oauth-token
  (let [db (db/create-db test_full_seed)
        h (make-handler (create-api db))
        apikey "2fvmer3qbk7f3jnqneg58bu2"
        td1 (assoc test_token_data :grant_type "authorization_code")
        req (-> (mock/request :post "/starbucks/oauth/token" )
                (update :headers assoc
                        "X-Api-Key" apikey
                        "Content-Type" "application/x-www-form-urlencoded"
                        "Accept" "application/json"
                        )
                (mock/query-string {"sig" "1234567"})
                (assoc :body (json/generate-string td1)))
        response @(h req)
        {:keys [status body error]} response
        ]
    ;; (log/debugf "oauth-token: response = %s" response)
    ;; (log/debugf "oauth-token: body = %s" (json/parse-string (b/to-string body) true))
    (log/debugf "oauth-token: error = %s" error)
    (log/debugf "oauth-token: status = %d" status)
    ;; (is (= 204 (:status response)))
    ;; (is (nil? (:body response)))
    ))

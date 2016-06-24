(ns rebujito.api.resources.address-test
  (:require
   [schema.core :as s]
   [schema-generators.generators :as g]
   [byte-streams :as bs]
   [bidi.bidi :as bidi]
   [rebujito.config :refer (config)]
   [rebujito.protocols :as p]
   [schema-generators.generators :as g]
   [cheshire.core :as json]
   [manifold.deferred :as d]
   [rebujito.protocols :as p]
   [rebujito.api-test :refer (print-body create-digital-card parse-body)]
   [rebujito.base-test :refer (system-fixture *system* *user-access-token* get-path  access-token-application access-token-user new-account-sb create-account new-sig  api-config)]
   [aleph.http :as http]
   [rebujito.api.resources
    [payment :as payment]
    [addresses :as addresses]
    [card :as card]
    [account :as account]
    [oauth :as oauth]
    [login :as login]]
   [clojure.test :refer :all]))


(use-fixtures :each (system-fixture #{:+mock-mimi :+ephemeral-db}))

(deftest address-testing
  (testing ::addresses/addresses
    (let [port (-> *system*  :webserver :port)
          address-id (atom "")]
      ;; get all
      (let [path (get-path ::addresses/addresses)
            http-response @(http/get (format "http://localhost:%s%s?access_token=%s"  port path *user-access-token*)
                                     {:throw-exceptions false
                                      :body-encoding "UTF-8"
                                      :content-type :json})
            body (parse-body http-response)
            ]
        (is (= 200 (-> http-response :status)))
        (is (= [] body)))

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
        (reset! address-id (last (clojure.string/split (:location (clojure.walk/keywordize-keys (-> http-res :headers ))) #"\/")))
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
        (is (= '(:addressLine1 :city :addressId :firstName :type :addressLine2 :lastName :postalCode :phoneNumber :country) (keys body))))

      ;; delete
      (let [path (bidi/path-for (-> *system* :docsite-router :routes) ::addresses/get :address-id @address-id)
            http-response @(http/delete (format "http://localhost:%s%s?access_token=%s"  port path  *user-access-token*)
                                        {:throw-exceptions false
                                         :body-encoding "UTF-8"
                                         :content-type :json})
            body (parse-body http-response)
            ]

        (is (= 200 (-> http-response :status)))
        (is (= '("OK" "Success" true) body))


        )
      ;; get-details now doesn't exist after deleting it
      (let [path (bidi/path-for (-> *system* :docsite-router :routes) ::addresses/get :address-id @address-id)
            http-response @(http/get (format "http://localhost:%s%s?access_token=%s"  port path *user-access-token*)
                                     {:throw-exceptions false
                                      :body-encoding "UTF-8"
                                      :content-type :json})
            body (parse-body http-response)]
        (is (= 400 (-> http-response :status))))



      ;;create-for-update
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
        (reset! address-id (last (clojure.string/split (:location (clojure.walk/keywordize-keys (-> http-res :headers ))) #"\/")))
        )
      (let [path (bidi/path-for (-> *system* :docsite-router :routes) ::addresses/get :address-id @address-id)
         http-response @(http/put (format "http://localhost:%s%s?access_token=%s"  port path  *user-access-token*)
                                  {:throw-exceptions false
                                   :body-encoding "UTF-8"
                                   :content-type :json
                                   :body (-> addresses/schema :put g/generate (assoc :firstName "poes" :lastName "doos") json/generate-string)})

         address-in-db (p/get-address (-> *system* :user-store) (:_id (p/read-token (-> *system* :authenticator) *user-access-token*)) @address-id)
         body (parse-body http-response)]

     (is (= 200 (-> http-response :status)))
     (is (nil? body))
    ;  (is (= "poes" (-> address-in-db :firstName)))
    ;  (is (= "doos" (-> address-in-db :lastName)))
     ))
    )

  )

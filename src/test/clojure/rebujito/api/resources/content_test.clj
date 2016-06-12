(ns rebujito.api.resources.content-test
  (:require
   [rebujito.base-test :refer (system-fixture *app-access-token* *system* *user-access-token* get-path  access-token-application access-token-user new-account-sb create-account new-sig  api-config)]
   [aleph.http :as http]
   [clojure.pprint :refer (pprint)]
   [cheshire.core :as json]
   [clojure.test :refer :all]))

(use-fixtures :each (system-fixture #{:+mock-mimi :+ephemeral-db}))

(deftest test-terms
  (let [port (-> *system*  :webserver :port)
        r (-> *system* :docsite-router :routes)]

    (testing "path media monks send us"
      (let [path "/starbucks/v1/content/sitecore/content/FR/3rd%20Party%20Mobile%20Content/iOS-Account%2FTerms%20of%20Use"]
        (is (= 200 (-> @(http/get (format "http://localhost:%s%s?access_token=%s" port path *app-access-token*)
                                  {:throw-exceptions false
                                   :body-encoding "UTF-8"
                                   :content-type :json})
                       :status)))))

     (testing "a more sane path"
       (let [path "/starbucks/v1/content/sitecore/content/FR/3rd%20Party%20Mobile%20Content/iOS-Account/Terms%20of%20Use"]
         (is (= 200 (-> @(http/get (format "http://localhost:%s%s?access_token=%s" port path *app-access-token*)
                                   {:throw-exceptions false
                                    :body-encoding "UTF-8"
                                    :content-type :json})
                        :status)))))

  ))

(ns rebujito.mongo-test
  (:require
   [rebujito.config :refer (config)]
   [manifold.deferred :as d]
   [rebujito.protocols :as p]
   [rebujito.base-test :refer (system-fixture *system*)]
   [clojure.test :refer :all]
   [rebujito.mongo :refer (generate-account-id id>mimi-id)]))


(use-fixtures :each (system-fixture #{:+mock-mimi :+ephemeral-db}))





(deftest mongo-tests
  (let [api-config (:api (config :test))]
   (testing "ApiClient p/login"
     (->
      (p/login (:api-client-store *system*) "XXXX" (:secret api-config))
      (d/chain
       (fn [a]
         (is false)))
      (d/catch clojure.lang.ExceptionInfo (fn [e] (is (re-find #"invalid hexadecimal representation" (:message (ex-data e)))))))



     (->
      (p/login (:api-client-store *system*) (p/generate-id (:api-client-store *system*) "123") (:secret api-config))
      (d/chain
       (fn [a]
         (is false)))
      (d/catch clojure.lang.ExceptionInfo (fn [e] (is (re-find #"api client-id and client-secret" (:message (ex-data e)))))))

     (->
      (p/login (:api-client-store *system*) (:key api-config) (:secret api-config))
      (d/chain
       (fn [a]
         (is true)))
      (d/catch clojure.lang.ExceptionInfo (fn [e] (is false))))))


  (testing "ids"
    (doseq [seed ["42472395" "42485871"]]
      (let [t (generate-account-id seed)]
        (is (= seed (id>mimi-id (str t))))))))

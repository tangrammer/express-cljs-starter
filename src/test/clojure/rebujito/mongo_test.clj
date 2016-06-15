(ns rebujito.mongo-test
  (:require
   [schema-generators.generators :as g]
   [rebujito.config :refer (config)]
   [rebujito.schemas :as rs]
   [manifold.deferred :as d]
   [rebujito.protocols :as p]
   [rebujito.base-test :refer (system-fixture *system* *user-access-token*)]
   [clojure.test :refer :all]
   [rebujito.mongo :refer (generate-account-id id>mimi-id)]))


(use-fixtures :each (system-fixture #{:+mock-mimi :+ephemeral-db}))




(deftest user-store
  (testing :add-auto-reload
    (let [user-id (:_id (p/read-token (:authenticator *system*) *user-access-token*))]
      (is (nil? (:autoReload (p/find (:user-store *system*) user-id))))
      (is (p/add-auto-reload (:user-store *system*) user-id {} (g/generate rs/AutoReloadMongo)))
  ;    (println (p/add-auto-reload (:user-store *system*) user-id {} (g/generate rs/AutoReloadMongo)))
  ;    (println user-id)
      (clojure.pprint/pprint (p/find (:user-store *system*) user-id))
      (is (:autoReload (p/find (:user-store *system*) user-id)))
      (is (:active (:autoReload (p/find (:user-store *system*) user-id))))))

  (testing :add-payment-method
    (let [user-id (:_id (p/read-token (:authenticator *system*) *user-access-token*))]
      (is (nil? (:paymentMethods (p/find (:user-store *system*) user-id))))

      (is (p/add-new-payment-method (:user-store *system*) user-id (g/generate rs/PaymentMethodMongo)))

      (is (:paymentMethods (p/find (:user-store *system*) user-id)))
      (is (p/add-new-payment-method (:user-store *system*) user-id (g/generate rs/PaymentMethodMongo)))
      (is (:paymentMethods (p/find (:user-store *system*) user-id)))
      ))

  (testing :disable-auto-reload
    (let [user-id (:_id (p/read-token (:authenticator *system*) *user-access-token*))]
      (is (:autoReload (p/find (:user-store *system*) user-id)))
      (is (p/disable-auto-reload (:user-store *system*) user-id))
      (is (false? (:active (:autoReload (p/find (:user-store *system*) user-id)))))))




  )

(deftest mongo-tests
  (let [api-config (:api (:monks (config :test)))]
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

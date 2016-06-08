(ns rebujito.addresses-test
  (:require
   [clojure.test :refer :all]
   [rebujito.protocols :as p]
   [rebujito.mongo :as m]
   [rebujito.config :refer (config)]
   [rebujito.base-test :refer (system-fixture *system*)]
   [rebujito.api.resources.addresses :as addresses]))

(use-fixtures :each (system-fixture #{:+mock-mimi :+ephemeral-db}))

(def payload {:addressLine1 "djdjjd"
              :addressLine2 "address line 2"
              :city "sjdjjd"
              :country "ZA"
              :firstName "Hdhdheh"
              :lastName "Djdjjd"
              :phoneNumber "656565"
              :postalCode "1375"
              :type "Billing"})

(defn create-fake-user [user-store]
  (let [id (p/generate-id user-store "123123")
        user {:_id id}]
    (p/insert! user-store user)
    user))

(deftest insert-address-test
  (let [api-config (:api (config :test))
        user-store (:user-store *system*)]
    (testing "insert-address"
      (let [user (create-fake-user user-store)
            _ (addresses/insert-address user-store user payload)
            user (first (p/find user-store user))]
        (is (not (nil? user)))
        (let [addresses (:addresses user)]
          (is (= 1 (count addresses)))
          (is (= payload (first addresses))))))))

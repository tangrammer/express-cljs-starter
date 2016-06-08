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
  (let [id (m/generate-account-id "123123")
        user {:_id id}]
    (p/insert! user-store user)
    user))

(deftest insert-address-test
  (let [api-config (:api (config :test))
        user-store (:user-store *system*)]
    (testing "insert-address"
      (let [user (create-fake-user user-store)
            user-id (str (:_id user))
            uuid (addresses/insert-address user-store user-id payload)
            user (first (p/find user-store user))
            addresses (:addresses user)
            address (first addresses)]
            
        (is (not (nil? user)))
        (is (= 1 (count addresses)))
        (is (= (assoc payload :uuid uuid)
              address))))))

(ns mimi.tests
  (:require
    [mimi.data :refer [validate-create-customer-data validate-link-card-data]]
    [cljs.test :refer-macros [deftest is testing run-tests]]
    [cljs.nodejs :as nodejs]))

(def valid-create-customer-data
  {:firstname "juan"
   :lastname "dos"
   :email "123"
   :city "123"
   :region "123"
   :postalcode "123"
   :birth {:month "5"
           :dayOfMonth "31"}})

(deftest schema-check
  (is (nil? (validate-create-customer-data valid-create-customer-data)))
  (is (not (nil? (validate-create-customer-data {:arb :data}))))
  (is (not (nil? (validate-create-customer-data (assoc valid-create-customer-data :extra :field)))))
  (is (not (nil? (validate-create-customer-data (dissoc valid-create-customer-data :email)))))
  (is (not (nil? (validate-create-customer-data (assoc (dissoc valid-create-customer-data :email) :extra :field))))))

(deftest validate-link-card-data-test
  (is (not (nil? (validate-link-card-data ""))))
  (is (not (nil? (validate-link-card-data {:cardNumber 9623570901113, :customerId "44051647"}))))
  (is (nil? (validate-link-card-data {:cardNumber "9623570901113", :customerId "44051647"})))
  )

(defn run []
  (nodejs/enable-util-print!)
  (print "\n\nrunning tests...")
  (run-tests))

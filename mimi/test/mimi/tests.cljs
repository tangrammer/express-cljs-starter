(ns ^:figwheel-always mimi.tests
  (:require
    [mimi.data :refer [validate-create-customer-data]]
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

(nodejs/enable-util-print!)
(print "\n\nrunning tests...")
(cljs.test/run-tests)

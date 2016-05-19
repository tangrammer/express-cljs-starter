(ns mimi.data
  (:require [schema.core :as s :include-macros true]))

(def CreateCustomerData
  "A schema for a nested data type"
  {:firstname s/Str
   :lastname s/Str
   :password s/Str
  ;  :email s/Str
   :mobile s/Str
   :gender (s/enum "male" "female")
   :birthday s/Str
   :city s/Str
   :region s/Str
   :postalcode s/Str})
   ;  :address s/Str
   ;  :country s/Str

(def checker (s/checker CreateCustomerData))

(defn validate-create-customer-data [data]
  (checker data))

(ns mimi.data
  (:require [schema.core :as s :include-macros true]))

(def CreateCustomerData
  "schema for create account request"
  {:firstname s/Str
   :lastname s/Str
   :password s/Str
   :email s/Str
   :mobile s/Str
   :gender (s/enum "male" "female")
   :birthday s/Str
   :city s/Str
   :region s/Str
   :postalcode s/Str})
   ;  :address s/Str
   ;  :country s/Str

(def LinkCardData
  "schema for link card request"
  {:customerId s/Str
   :cardNumber s/Str})

(def customer-checker (s/checker CreateCustomerData))

(defn validate-create-customer-data [data]
  (customer-checker data))

(def link-card-checker (s/checker LinkCardData))

(defn validate-link-card-data [data]
  (link-card-checker data))

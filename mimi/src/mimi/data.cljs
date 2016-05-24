(ns mimi.data
  (:require [schema.core :as s :include-macros true]))

(def CreateCustomerData
  "schema for create account request"
  {:firstname s/Str
   :lastname s/Str
   :email s/Str
   :postalcode s/Str
   :city s/Str
   :region s/Str
   :birth {:dayOfMonth s/Str
           :month s/Str}})

(def LinkCardData
  "schema for link card request"
  {:customerId s/Str
   :cardNumber s/Str})

(def customer-checker (s/checker CreateCustomerData))

(defn validate-create-customer-data [data]
  (customer-checker data))

(def link-card-checker (s/checker LinkCardData))

(defn in-test-card-range [card-number]
  (if (re-matches #"96235709\d{5}" card-number)
    nil
    "test cardNumber must be in 96235709***** range"))

(defn validate-link-card-data [data]
  (or
    (link-card-checker data)
    (in-test-card-range (:cardNumber data))))

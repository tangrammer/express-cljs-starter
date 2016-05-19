(ns mimi.routes
  (:require [cljs.nodejs :as nodejs]
            [mimi.express :refer [app]]
            [mimi.log :as log]
            [mimi.config :as config]))

(def jwt (nodejs/require "express-jwt"))
(def micros (nodejs/require "micros"))
(def moment (nodejs/require "moment"))

(. micros (setBrand "starbucks"))

(def createMicrosCustomer (.-createCustomer micros))
(def linkCard (.-setCustomerPosRef micros))

(defn now-iso []
  (.format (moment) "YYYY-MM-DD HH:mm:ss.S"))

(.use app (.unless (jwt #js {:secret config/jwt-secret}) #js {:path #js ["/mimi/health"]}))

(.get app "/mimi/health" #(.send %2 "ok"))

(defn parse-customer-fields [req-body]
  (let [firstname (get req-body "firstname")
        lastname (get req-body "lastname")
        password (get req-body "password")
        email (get req-body "email")
        mobile (get req-body "mobile")
        address (get req-body "address")
        city (get req-body "city")
        region (get req-body "region")
        country (get req-body "country")
        postalcode (get req-body "postalcode")
        gender (get req-body "gender")
        birthday (get req-body "birthday")]
    {:firstname firstname
     :lastname lastname
     :password password
     :email email
     :mobile mobile
     :address address
     :city city
     :region region
     :country country
     :postalcode postalcode
     :gender gender
     :birthday birthday}))

(defn customer-details-from-payload [payload]
  {:firstname (get payload :firstname)
   :lastname (get payload :lastname)
   :mobilephonenumber (get payload :mobile)
   :emailaddress (get payload :email)
   :state (get payload :region)
   :city (get payload :city)
   :gender (first (get payload :gender))
   :birthday (str (get payload :birthday) " 00:00:00.0")
   :signupdate (now-iso)
   :createddate (now-iso)})

(.post app "/mimi/starbucks/account"
  (fn
    [req res]
    "create a starbucks customer in micros"
    (let [payload (parse-customer-fields (->> req .-body js->clj))
          details (customer-details-from-payload payload)]
      (prn payload)
      (prn details)
      (createMicrosCustomer (clj->js details)
        (fn [err result]
          (prn err result)
          ;; TODO send {:status "ok" :customerid "123"})
          (if err
            (do
              (. res (code 500))
              (. res (send err)))
            (.send res result)))))))

(defn parse-link-card [req-body]
  (let [customer-id (get req-body "customerId")
        card-number (get req-body "cardNumber")]
    {:customer-id customer-id
     :card-number card-number}))

(.post app "/mimi/starbucks/account/card"
  (fn
    [req res]
    "link card to account"
    (let [fields (parse-link-card (->> req .-body js->clj))
          customer-id (:customer-id fields)
          card-number (:card-number fields)]
      (log/debug "got fields")
      (prn fields)
      (linkCard customer-id card-number
        (fn [err result]
          (prn err result)
          (if err
            (do
              (. res (status 500))
              (. res (send err)))
            (.send res result)))))))

(ns mimi.routes
  (:require [cljs.nodejs :as nodejs]
            [mimi.express :refer [app]]
            [mimi.log :as log]))

(def jwt (nodejs/require "express-jwt"))
(def micros (nodejs/require "micros"))
(def moment (nodejs/require "moment"))

(. micros (setBrand "starbucks"))

(def createMicrosCustomer (.-createCustomer micros))

(defn now-iso []
  (.format (moment) "YYYY-MM-DD HH:mm:ss.S"))

; (.use app (.unless (jwt #js {:secret config/jwt-secret}) #js {:path #js ["/mimi/health"]}))

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
          (.send res result))))))

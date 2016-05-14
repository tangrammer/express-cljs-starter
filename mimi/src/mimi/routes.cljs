(ns mimi.routes
  (:require [cljs.nodejs :as nodejs]
            [mimi.express :refer [app]]
            [mimi.log :as log]))

(def jwt (nodejs/require "express-jwt"))

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
        postalcode (get req-body "postalcode")]
    {:firstname firstname
     :lastname lastname
     :password password
     :email email
     :mobile mobile
     :address address
     :city city
     :region region
     :country country
     :postalcode postalcode}))

(.post app "/mimi/starbucks/account"
  (fn
    [req res]
    "create a starbucks customer in micros"
    (let [payload (parse-customer-fields (->> req .-body js->clj))]
      ; (log/debug "got" payload)
      (prn payload)
      (.send res "ok"))))

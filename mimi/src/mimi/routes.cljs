(ns mimi.routes
  (:require [cljs.nodejs :as nodejs]
            [clojure.set :refer [rename-keys]]
            [mimi.express :refer [app]]
            [mimi.log :as log]
            [mimi.config :as config]
            [mimi.data :refer [validate-create-customer-data validate-link-card-data]]))

(def jwt (nodejs/require "express-jwt"))
(def micros (nodejs/require "micros"))
(def moment (nodejs/require "moment"))

(def app-version (nodejs/require "../../VERSION.json"))
(def service (.-service app-version))
(def version (.-version app-version))
(def built (.-built app-version))

(. micros (setBrand "starbucks"))

(def create-micros-customer (.-createCustomer micros))
(def link-card (.-setCustomerPosRef micros))
(def get-balances (.-getStarbucksBalances micros))

(defn now-iso []
  (.format (moment) "YYYY-MM-DD HH:mm:ss.S"))

(defn valid-birthday [month day]
  (let [strict true
        leap-year "2004"]
    (.isValid (moment (str leap-year "-" month "-" day) "YYYY-M-D" strict))))

(.use app (.unless (jwt #js {:secret config/jwt-secret}) #js {:path #js ["/mimi/health"]}))

(.get app "/mimi/health" #(.json %2 #js {:service service :version version :built built}))

(defn micros-transform [payload]
  (merge
    (rename-keys (dissoc payload :birth)
      {:email :emailaddress
       :mobile :mobilephonenumber
       :region :state})
    {:gender (first (:gender payload))
     :birthdayofmonth (get-in payload [:birth :dayOfMonth])
     :birthmonth (get-in payload [:birth :month])
     :signupdate (now-iso)
     :createddate (now-iso)
     :miscfield4 "test account"}))

(def invalid-payload {:error "invalid payload"
                      :link "http://bit.ly/_mimi"})

(.post app "/mimi/starbucks/account"
  (fn
    [req res]
    "create a starbucks customer in micros"
    (let [customer-data (-> req .-body (js->clj :keywordize-keys true))
          birth (:birth customer-data)
          validation-errors (validate-create-customer-data customer-data)
          valid-birthday (valid-birthday (:month birth) (:dayOfMonth birth))
          validation-birthday (if valid-birthday nil {:birth "invalid birth day/month combo"})
          customer-data (micros-transform customer-data)]
      (log/info "create customer")
      (prn customer-data)
      (if (or validation-errors (not valid-birthday))
        (.json (.status res 400)
           (clj->js (assoc invalid-payload :details (or validation-errors validation-birthday))))
        (let [customer-data-js (clj->js customer-data)]
          (log/info "create-micros-customer" customer-data-js)
          (create-micros-customer customer-data-js
            (fn [err result]
              (log/debug "got result from micros: err" err "result" result)
              (if err
                (.json (.status res 500) #js {:error (.toString err)})
                (.json res #js {:status "ok"
                                :customerId (aget result 0)})))))))))

(def codez {:tier (clj->js {:code "MSR001"})
            :reward (clj->js {:code "MSR002"})
            :stored-value-card (clj->js {:code "SGC001"})})

(.post app "/mimi/starbucks/account/card"
  (fn
    [req res]
    "link card to account"
    (let [payload (-> req .-body (js->clj :keywordize-keys true))
          validation-errors (validate-link-card-data payload)
          customer-id (:customerId payload)
          card-number (:cardNumber payload)]
      (log/info "link card")
      (prn payload)
      (if validation-errors
        (.json (.status res 400) (clj->js (assoc invalid-payload :details validation-errors)))
        (do

          (print "creating accounts for" card-number)

          ; TODO async this code
          (.createSingleAccountType micros
            (:tier codez)
            card-number
            (fn [err]
              (if err (log/error "can't create MSR tier" err))))

          (.createSingleAccountType micros
            (:stored-value-card codez)
            card-number
            (fn [err]
              (if err (log/error "can't create stored value card" err))))

          (.createSingleAccountType micros
            (:reward codez)
            card-number
            (fn [err]
              (if err (log/error "can't create stored value card" err))))

          (link-card customer-id card-number
            (fn [err result]
              (log/debug "got result from micros: err" err "result" result)
              (if err
                (.json (.status res 500) #js {:error (.toString err)})
                (.json res #js {:status "ok"})))))))))

(.get app "/mimi/starbucks/account/:cardNumber/balances"
  (fn
    [req res]
    "get account balances"
    (let [card-number (.param req "cardNumber")]
      (log/info "getting balances for" card-number)
      (.then (get-balances card-number)
        (fn [result]
          (log/debug "got result from micros:" result)
          (.json res (clj->js result)))
        (fn [err]
          (.json (.status res 500) #js {:error (.toString err)}))))))

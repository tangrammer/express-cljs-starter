(ns mimi.routes
  (:require [cljs.nodejs :as nodejs]
            [mimi.express :refer [app]]
            [mimi.log :as log]
            [mimi.config :as config]
            [mimi.data :refer [validate-create-customer-data validate-link-card-data]]))

(def jwt (nodejs/require "express-jwt"))
(def micros (nodejs/require "micros"))
(def moment (nodejs/require "moment"))

(. micros (setBrand "starbucks"))

(def create-micros-customer (.-createCustomer micros))
(def link-card (.-setCustomerPosRef micros))

(defn now-iso []
  (.format (moment) "YYYY-MM-DD HH:mm:ss.S"))

(defn valid-birthday [birthday-str]
  (let [strict true]
    (.isValid (moment birthday-str "YYYY-MM-DD" strict))))

(.use app (.unless (jwt #js {:secret config/jwt-secret}) #js {:path #js ["/mimi/health"]}))

(.get app "/mimi/health" #(.send %2 "ok"))

(defn fill-in [payload]
  (merge payload
    {:gender (first (get payload :gender))
     :birthday (str (get payload :birthday) " 00:00:00.0")
     :signupdate (now-iso)
     :createddate (now-iso)}))

(def invalid-payload {:error "invalid payload"
                      :link "http://bit.ly/_mimi"})

(.post app "/mimi/starbucks/account"
  (fn
    [req res]
    "create a starbucks customer in micros"
    (let [customer-data (-> req .-body (js->clj :keywordize-keys true))
          valid-birthday (valid-birthday (:birthday customer-data))
          validation-errors (validate-create-customer-data customer-data)
          customer-data (fill-in customer-data)]
      (log/info "create customer")
      (prn customer-data)
      (if (or validation-errors (not valid-birthday))
        (.json (.status res 400)
           (clj->js (assoc invalid-payload :details (or validation-errors "birthday format is YYYY-MM-DD"))))
        (let [customer-data-js (clj->js customer-data)]
          (log/info "create-micros-customer" customer-data-js)
          (create-micros-customer customer-data-js
            (fn [err result]
              (log/debug "got result from micros: err" err "result" result)
              (if err
                (.json (.status res 500) #js {:error (.toString err)})
                (.json res #js {:status "ok"
                                :customerId (aget result 0)})))))))))

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
      (log/debug "stuff?" (clj->js (assoc invalid-payload :details validation-errors)))
      (if validation-errors
        (.json (.status res 400) (clj->js (assoc invalid-payload :details validation-errors)))
        (link-card customer-id card-number
          (fn [err result]
            (log/debug "got result from micros: err" err "result" result)
            (if err
              (.json (.status res 500) #js {:error (.toString err)})
              (.json res #js {:status "ok"}))))))))

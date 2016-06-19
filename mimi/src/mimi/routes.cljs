(ns mimi.routes
  (:require [cljs.nodejs :as nodejs]
            [clojure.set :refer [rename-keys]]
            [mimi.express :refer [app]]
            [mimi.log :as log]
            [mimi.config :as config]
            [mimi.data :refer [validate-create-customer-data validate-link-card-data]]))

(def jwt (nodejs/require "express-jwt"))
(def micros (nodejs/require "micros"))
(def starbucks-micros (nodejs/require "micros/lib/starbucks"))
(def moment (nodejs/require "moment"))
(def promise (nodejs/require "bluebird"))
(def inspect (.-inspect (nodejs/require "util")))

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
        (let [promise (.ensureAccountExists starbucks-micros card-number)]
          (log/info "creating accounts for" card-number)

          (let [p2 (.then promise
                      (fn []
                        (log/info "linking card" card-number "to" customer-id)
                        (link-card customer-id card-number)))]
            (.then p2
              (fn []
                (log/info "great success for linking card" card-number)
                (.json res #js {:status "ok"})))

            (.catch p2
              (fn [err]
                (log/error "creating/linking account" err)
                (.json (.status res 500) #js {:error (.toString err)})))))))))

(.get app "/mimi/starbucks/account/:cardNumber/balances"
  (fn [req res]
    (let [card-number (-> req .-params .-cardNumber)
          p0 (get-balances card-number)
          p1 (.listCoupons micros card-number)
          p2 (.all promise #js [p0 p1])]
      (log/info "getting balances and coupons for" card-number)
      (.then p2
        (fn [result]
          (log/debug "got result from micros:" (inspect result #js {:depth nil}))
          (let [rewards (js->clj (get result 0))
                coupons (js->clj (get result 1))
                out (merge rewards {:coupons coupons})]
            (.json res (clj->js out))))

        (fn [err]
          (.json (.status res 500) #js {:error (.toString err)}))))))

(.post app "/mimi/starbucks/account/:cardNumber/:cardType"
  (fn
    [req res]
    "issue points into a card number and type"
    (let [card-number (-> req .-params .-cardNumber)
          card-type (-> req .-params .-cardType)
          amount (-> req .-body .-amount)
          p1 (.issuePoints starbucks-micros (clj->js {:account card-number :code card-type :amount amount}))
          p2 (.then p1 #(get-balances card-number))]

      (log/info "issuing" amount "points to" card-number ":" card-type)

      (.then p2
        (fn [result]
          (let [result (js->clj result :keywordize-keys true)
                programs (:programs result)
                program (first (filter #(= (:code %) card-type) programs))]
            (log/debug "response" #js {:balance (:balance program)})
            (.json res #js {:balance (:balance program)}))))

      (.catch p2
        (fn [err]
          (log/error "issuing points" err)
          (.json (.status res 500) #js {:error (.toString err)}))))))

(.get app "/mimi/starbucks/account/:cardNumber/transactions"
  (fn
    [req res]
    "get transactional history"
    (let [card-number (-> req .-params .-cardNumber)
          p0 (.transactions micros #js {:account card-number})]
      (.then p0
        (fn [result]
          (.json res #js {:transactions result})))

      (.catch p0
        (fn [err]
          (log/error "getting transactions" err)
          (.json (.status res 500) #js {:error (.toString err)}))))))


(.get app "/mimi/starbucks/account/:cardNumber/coupons"
  (fn
    [req res]
    "get coupons"
    (let [card-number (-> req .-params .-cardNumber)
          p0 (.listCoupons micros card-number)]
      (doto p0
        (.then
          (fn [result]
            (.json res #js {:coupons result})))
        (.catch
          (fn [err]
            (log/error "getting coupons" err)
            (.json (.status res 500))))))))

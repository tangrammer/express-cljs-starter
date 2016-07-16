(ns rebujito.mimi
  (:require
   [clojure.core.async :as async :refer [<! >! timeout]]
   [manifold.deferred :as d]
   [rebujito.protocols :as protocols]
   [rebujito.util :refer (dtry ddtry)]
   [rebujito.schemas :refer (MimiUser)]
   [clj-http.client :as http-c]
   [org.httpkit.client :as http]
   [cheshire.core :as json]
   [byte-streams :as bs]
   [com.stuartsierra.component  :as component]
   [schema.core :as s]
   [taoensso.timbre :as log]))

(defn random-value []
  (first (random-sample 0.1 (cycle [{:status 500 :body :fail-message} {:status 500 :body :fail-message}{:status 500 :body :fail-message} {:status 200 :body :success!}]))))

(defn repeat-and-delay [http-fn* attempts time-to-delay  ex-data-fn]
  (let [c (async/chan)
        rchan (d/deferred)]
    (async/go-loop [attempt-number 0]
      (let [res (async/<! c)]
        (log/debug "Attempt: "  attempt-number " :: Res: " res)
        (if (>= (:status res) 400)
          (do
            (async/<!! (async/timeout time-to-delay))
            (if (> attempts attempt-number)
              (do (http-fn* c)
                (recur (inc attempt-number)))
              (d/error! rchan  (ex-info (format "Mimi exception after %s retries " attempts) (ex-data-fn res)))))
          (d/success! rchan (:body res)))))
    (http-fn* c)
    rchan))

(defn http-call
  [c]
  (async/thread
    (async/<!! (async/timeout 1000))
    (let [http-res (random-value)]
      (println "http res: " http-res)
      (async/>!! c http-res))))

#_(do
  (println @(repeat-and-delay  http-call 1 100 (fn [res] (merge res {:type :mimi :code 134 :message "wow!"})))))

(def errors {:create-account {"111000" [400 "Username is already taken" "Account Management Service returns error that user name is already taken"]
                              "111001" [400"Unknown error occured" "Account Management Service returns error"]
                              "111005" [400 "Email address too long. Must be 50 characters or less." ""]
                              "111008" [400 "Please supply an email address" "Missing emailAddress attribute."]
                              "111009" [400 "Please supply a registration source" "Missing registration source attribute."]
                              "111011" [400 "Please supply a password" "Missing password attribute"]
                              "111012" [400 "Please supply a market" "Missing market parameter is required"]
                              "111015" [400 "Please supply a last name" "Missing lastName attribute"]
                              "111016" [400 "Please supply a first name" "Missing firstName attribute"]
                              "111022" [400 "Password does not meet complexity requirements" "Account Management Service returns error that password does not meet complexity requirements"]
                              "111023" [400 "No Request supplied" "Create Account Request was malformed."]
                              "111025" [400 "Invalid postalCode. No results were resolved from geolookup" "Return this message when no results are found when resolving address from postal code when market = US"]
                              "111027" [400 "Email address is not unique" "Account Management Service returns error that email address is already taken"]
                              "111036" [400 "Invalid characters (Ø, ø, Ë, ë) specified for first and/or last name." ""]
                              "111039" [400 "Invalid market code or country code." ""]
                              "111041" [400 "Invalid email address" "Email address was malformed"]
                              "111046" [400 "firstName failed profanity check."]
                              "500"    [500 "An unexpected error occurred processing the request."]
                              }})

(defn new-error [m [status & args]]
  (ex-info (str m ) {:type :mimi
                     :status status
                     :body args}))

(def GenderSchema (s/enum "male" "female"))

(def CreateAccountSchema
  {
   :birth {:dayOfMonth  String
           :month       String}
   :city String
   :email String
   :firstname String
   ;:gender GenderSchema
   :lastname String
   ;:mobile String
   ;:password String
   :postalcode String
   :region String
   })

(defn- get-code [type]
  (condp = type
    :stored-value "SGC001"
    :loyalty "MSR001"
    :rewards "MSR002"
    (throw (ex-info (format "MIMI: There's no code for type %s" type) {}))))

(def urls
  {:issue-coupon
   (fn [base-url card-number coupon-type]
     {:method :post
      :url (format "%s/account/%s/coupons/%s/issue" base-url card-number coupon-type)})
   :transfer-card
    (fn [base-url]
      {:method :post
       :url (format "%s/account/transfer" base-url)})
   :transactions
     (fn [base-url card-number]
       {:method :get
        :url (format "%s/account/%s/transactions" base-url card-number)})
   :balances
     (fn [base-url card-number]
       {:method :get
        :url (format "%s/account/%s/balances" base-url card-number)})
   :create-account
     (fn [base-url]
       {:method :post
        :url (format "%s/account" base-url)})
   :link-card
     (fn [base-url]
       {:method :post
        :url (format "%s/account/card" base-url)})
   :increment-balance
     (fn [base-url card-number card-type-code]
       {:method :post
        :url (format "%s/account/%s/%s" base-url card-number card-type-code)})
  })

(defn call-mimi
  ([token url-method] (call-mimi token url-method {}))
  ([token url-method data]
   (http-c/request (merge url-method
                    {:headers {"Authorization" (format "Bearer %s" token)}
                    :form-params data
                    :insecure? true
                    :content-type :json
                    :accept :json
                    :as :json
                    :throw-exceptions true
                    }))))

(defrecord ProdMimi [base-url token]
  component/Lifecycle
  (start [this]
    this)
  (stop [this] this)
  protocols/Mimi
  (create-account [this data]
    (log/info "create-account-mimi: " (format "%s/account" base-url) data)
    (let [d* (d/deferred)]
      (d/future
        (let [url ((:create-account urls) base-url)
              try-type :mimi
              try-id ::create-account
              try-context '[data url]]
          (ddtry d* (do
                      (s/validate CreateAccountSchema data)
                      (let [{:keys [status body]} (call-mimi token url data)]
                       (log/info "mimi create-account" body)
                       (-> body :customerId vector (conj :prod-mimi))))
                 )))
      d*))
  (update-account [this data]
    (log/warn "update-account-mimi! [_ data]" data)
    (clj-bugsnag.core/notify
     (rebujito.MimiTODOException. "TODO: MIMI update-account is not implemented yet!")
          {:api-key (:key (:bugsnag (rebujito.config/config)))
           :environment rebujito.util/*bugsnag-release*
           :meta {:context {:data data}}})
    true)


      ; TODO rename to link-card
  (register-card [this data]
    (log/info (format "%s/account/card" base-url))
    (log/info data)
    (let [d* (d/deferred)]
      (d/future
        (let [url ((:link-card urls) base-url)
              try-type :mimi
              try-id ::register-card
              try-context '[data url]]
          (ddtry d*
                (let [{:keys [status body]} (call-mimi token url data)]
                  (log/info "mimi register-card" status body)
                  (-> [:success]
                      (conj :prod-mimi))))))
      d*))

  (increment-balance! [this card-number amount type]
    (log/debug "(increment-balance! [_ card-number amount type])" card-number amount type)
    (let [d* (d/deferred)
          card-type-code (get-code type)
          try-id ::increment-balance
          try-type :mimi
          try-context '[card-number amount card-type-code type]]
      (d/future
        (ddtry d* (do
                    (log/info "loading" card-number "with" amount (format "%s/account/%s/" base-url card-number card-type-code))
                    (let [url-method ((:increment-balance urls) base-url card-number card-type-code)
                          {:keys [status body]} (call-mimi token url-method {:amount amount})]
                      (log/info "mimi increment-balance " status body)
                       {:balance (:balance body)}))))
      d*))

  (balances [this card-number]
    (let [url ((:balances urls) base-url card-number)
          try-type :mimi
          try-id ::balances
          try-context '[url card-number]]
      (dtry (do (log/info "fetching prod balances for" card-number)
                (repeat-and-delay
                 #(d/future
                    (try
                      (let [{:keys [status body] :as all} (call-mimi token url)]
                        (log/info "mimi balances: " body)
                        (async/>!! % all))
                      (catch Exception e (async/>!! % {:status 500
                                                       :body (.getMessage e)}))
                      ))
                 3 100 (fn [res] (merge res {:type :mimi :code "xxxxx" :message "Balances error!"}))
                 )))))

  (get-history [this card-number]
    (log/info "fetching transactions for" card-number)
    (let [d* (d/deferred)]
      (d/future
        (let [url ((:transactions urls) base-url card-number)
              try-type :mimi
              try-id ::get-history
              try-context '[card-number url]]
          (ddtry d*
                 (let [{:keys [status body]} (call-mimi token url)]
                   (log/info "mimi get-history" body)
                   body))))
      d*))

  (transfer [this from to]
    (log/info "transferring card balances from" from "to" to)
    (let [d* (d/deferred)]
      (d/future
        (let [url ((:transfer-card urls) base-url)
              try-type :mimi
              try-id ::transfer
              try-context '[url from to]]
          (ddtry d*
                 (let [{:keys [status body]} (call-mimi token url {:from from :to to})]
                   (log/info "mimi transfer" body)
                   body))))
    d*))

  (issue-coupon [this card-number coupon-type]
    (log/info "issuing" coupon-type "coupon to" card-number)
    (call-mimi token ((:issue-coupon urls) base-url card-number coupon-type)))
  )

(defn new-prod-mimi [mimi-config]
  (map->ProdMimi mimi-config))

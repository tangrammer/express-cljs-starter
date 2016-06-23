(ns rebujito.mimi
  (:require
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
   [rebujito.store.mocks :as mocks]
   [taoensso.timbre :as log]))

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
        (let [try-type :mimi
              try-id ::create-account
              try-context '[data]]
          (ddtry d* (do
                      (s/validate CreateAccountSchema data)
                      (let [{:keys [status body]} (http-c/post (format "%s/account" base-url)
                                                              {:headers {"Authorization" (format "Bearer %s" token)}
                                                               :insecure? true
                                                               :content-type :json
                                                               :accept :json
                                                               :as :json
                                                               :throw-exceptions true
                                                               :form-params data})]
                       (log/info body)
                       (-> body :customerId vector (conj :prod-mimi))))
                 )))
      d*))
      ; TODO rename to link-card
  (register-physical-card [this data]
    (log/info (format "%s/account/card" base-url))
    (log/info data)
                                        ;    (println {"Authorization" (format "Bearer %s" token)})
    (let [d* (d/deferred)]
      (d/future
        (let [try-type :mimi
              try-id ::register-physical-card
              try-context '[data]]
          (ddtry d*
                (let [{:keys [status body]}
                      (http-c/post (format "%s/account/card" base-url) {
                                        ;                                 :method :post
                                                                        :insecure? true
                                                                        :headers {"Authorization" (format "Bearer %s" token)}
                                                                        :form-params  data
                                                                        :content-type :json
                                                                        :accept :json
                                                                        :as :json
                                                                        }
                                   )]
                  (log/info status body)
                  (-> [:success]
                      (conj :prod-mimi))))))
      d*))

  (load-card [this card-number amount]
    (let [d* (d/deferred)
          try-id ::load-card
          try-type :mimi
          try-context '[card-number amount]]
      (d/future
        (ddtry d* (do
                    (log/info "loading" card-number "with" amount (format "%s/account/%s/SGC001" base-url card-number))
                    (let [{:keys [status body]}
                          (http-c/post (format "%s/account/%s/SGC001" base-url card-number)
                                       {:insecure? true
                                        :headers {"Authorization" (format "Bearer %s" token)}
                                        :form-params {:amount amount}
                                        :content-type :json
                                        :accept :json
                                        :as :json}
                                       )]
                      (log/info "load-card-mimi response " status body)
                       {:balance (:balance body)}))))
      d*))

  (balances [this card-number]
    (log/info "fetching rewards for" card-number)
    (let [d* (d/deferred)]
      (d/future
      (try
        (let [{:keys [status body]} (http-c/get (format "%s/account/%s/balances" base-url card-number)
                                                {:headers {"Authorization" (format "Bearer %s" token)}
                                                 :insecure? true
                                                 :content-type :json
                                                 :accept :json
                                                 :as :json
                                                 :throw-exceptions true
                                                 :form-params {}})]
          (log/info body)
          (d/success! d* body))
        (catch clojure.lang.ExceptionInfo e (let [ex (ex-data e)]
                                              (d/error! d* (ex-info (str "error!!!" (:status ex))
                                                                    {:type :mimi
                                                                      :status (:status ex)
                                                                      :body (:body ex)}))))
          (catch Exception e (d/error! d* (ex-info (str "error!!!" 500)
                                                   {:type :mimi
                                                    :status 500
                                                    :body (.getMessage e)})))
        ))
      d*))



  (get-history [this card-number]
    (log/info "fetching transactions for" card-number)
    (let [d* (d/deferred)
          ;; card-number "9623570900002"
          ]
      (d/future
      (try
        (let [{:keys [status body]} (http-c/get (format "%s/account/%s/transactions" base-url card-number)
                                                {:headers {"Authorization" (format "Bearer %s" token)}
                                                 :insecure? true
                                                 :content-type :json
                                                 :accept :json
                                                 :as :json
                                                 :throw-exceptions true
                                                 :form-params {}})]
          (log/info body)
          (d/success! d* body))
        (catch clojure.lang.ExceptionInfo e (let [ex (ex-data e)]
                                              (d/error! d* (ex-info (str "error!!!" (:status ex))
                                                                    {:type :mimi
                                                                      :status (:status ex)
                                                                      :body (:body ex)}))))
          (catch Exception e (d/error! d* (ex-info (str "error!!!" 500)
                                                   {:type :mimi
                                                    :status 500
                                                    :body (.getMessage e)})))
        ))
      d*))
  )

(defrecord MockMimi [base-url token]
  component/Lifecycle
  (start [this]
    this)
  (stop [this] this)
  protocols/Mimi
  (create-account [this data]
    (let [d* (d/deferred)]
      (d/success! d* (-> (str (rand-int 1000000))
                         vector
                         (conj :mock-mimi)))

      d*))
  (register-physical-card [this data]
    (log/info (format "%s/account/card" base-url))
    (log/info data)

    (let [d* (d/deferred)]
      (d/future
        (d/success! d* (-> [:success]
                           (conj :prod-mimi))))
      d*))
  (load-card [this card-number amount]
    (let [d* (d/deferred)]
      (if-let [mimi-card-data (assoc mocks/mimi-card :target-environment :dev) #_(p/load-card mimi {:cardId (-> card-data :cardNumber)
                                      :amount (-> payment-data :amount)})]
        (d/success! d* mimi-card-data)
        (d/error! d* (ex-info (str "API MIMI ERROR")
                              {:type :mimi
                               :status 500
                               :body ["An unexpected error occurred debiting the card."]})))
      d*)
    )

  (balances [this card-number]
    ; TODO: mock the response, don't hit mimi
    (log/info "fetching rewards for" card-number)
    (let [d* (d/deferred)
          ; card-number "9623570800099"
          card-number "9623570900002"
          ]
      (d/future
        (try
          (let [{:keys [status body]} (http-c/get (format "%s/account/%s/balances" base-url card-number)
                                                  {:headers {"Authorization" (format "Bearer %s" token)}
                                                   :insecure? true
                                                   :content-type :json
                                                   :accept :json
                                                   :as :json
                                                   :throw-exceptions true
                                                   :form-params {}})]
            (log/info body)
            (d/success! d* body))
          (catch clojure.lang.ExceptionInfo e (let [ex (ex-data e)]
                                                (d/error! d* (ex-info (str "error!!!" (:status ex))
                                                                      {:type :mimi
                                                                        :status (:status ex)
                                                                        :body (:body ex)}))))
          (catch Exception e (d/error! d* (ex-info (str "error!!!" 500)
                                                   {:type :mimi
                                                    :status 500
                                                    :body (.getMessage e)})))
          ))
      d*))
  (get-history [this card-number])
  )

(defn new-prod-mimi [mimi-config]
  (map->ProdMimi mimi-config))

(defn new-mock-mimi [mimi-config]
  (map->MockMimi  mimi-config))

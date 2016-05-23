(ns rebujito.mimi
  (:require
   [manifold.deferred :as d]
   [rebujito.protocols :as protocols]
   [clj-http.client :as http-c]
   [org.httpkit.client :as http]
   [cheshire.core :as json]
   [byte-streams :as bs]
   [com.stuartsierra.component  :as component]
   [schema.core :as s]
   [rebujito.store.mocks :as mocks]
   [taoensso.timbre :as log]))

(def errors {:create-account {"111000" [400 "Username is already taken" "Account Management Service returns error that user name is already taken"]
                              "111001" [400"Unknown error occured" "Account Management Service returns error
"]
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
   :gender GenderSchema
   :lastname String
   :mobile String
   :password String
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
    (log/info (format "%s/account" base-url))
    (log/debug data)
;    (println {"Authorization" (format "Bearer %s" token)})
    (let [d* (d/deferred)]
      (d/future
        (try
          (let [{:keys [status body]} (http-c/post (format "%s/account" base-url)
                                                   {:headers {"Authorization" (format "Bearer %s" token)}
                                                    :insecure? true
                                                    :content-type :json
                                                    :accept :json
                                                    :as :json
                                                    :throw-exceptions true
                                                    :form-params data})]
               (log/debug status body)
               (d/success! d* (-> body
                                  :customerId
                                  vector
                                  (conj :prod-mimi))))
          (catch clojure.lang.ExceptionInfo e (let [ex (ex-data e)]
                                                (d/error! d* (ex-info (str "error!!!" (:status ex))
                                                                      {:type :mimi
                                                                       :status (:status ex)
                                                                       :body (:body ex)}))))))
      d*)))

(defrecord MockMimi [base-url token]
  component/Lifecycle
  (start [this]
    this)
  (stop [this] this)
  protocols/Mimi
  (create-account [this data ]
    (let [d* (d/deferred)]
      (d/error! d* (new-error "testing " (-> errors :create-account (get "111000"))))
      d*)))

(defn new-prod-mimi [mimi-config]
  (map->ProdMimi mimi-config))

(defn new-mock-mimi [mimi-config]
  (map->MockMimi  mimi-config))

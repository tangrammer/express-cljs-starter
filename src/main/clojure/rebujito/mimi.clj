(ns rebujito.mimi
  (:require
   [manifold.deferred :as d]
   [rebujito.protocols :as protocols]
   [clj-http.client :as http-c]
   [org.httpkit.client :as http]
   [cheshire.core :as json]
   [byte-streams :as bs]
   [com.stuartsierra.component  :as component]
   [rebujito.store.mocks :as mocks]))


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


(def CreateAccountSchema
  {
   :address String
   :birthday String ;; 'YYYY-MM-DD'
   :city String
   :country String
   :email String
   :firstname String
   :gender String ;; (male|female)
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
    (println (format "%s/account" base-url))
    (println {"Authorization" (format "Bearer %s" token)})
    (let [d* (d/deferred)]
      (d/future (when-let [[status body] (let [{:keys [status body]}
                                               (http-c/post (format "%s/account" base-url)
                                                            {:headers {"Authorization" (format "Bearer %s" token)}
                                                             :insecure? true
                                                             :content-type :json
                                                             :accept :json
                                                             :as :json
                                                             :throw-exceptions true
                                                             :form-params {:firstname "Juan A."
                                                                           :lastname "Ruz"
                                                                           :password "xxxxxx"
                                                                           :email "juanantonioruz@gmail.com"
                                                                           :mobile "0034630051897"
                                                                           :city "Sevilla"
                                                                           :region "Andalucia"
                                                                           :postalcode "41003"
                                                                           :gender "male" ; (male|female)
                                                                           :birth {:dayOfMonth  "01"
                                                                                   :month       "01"}}})]
                                           [status (-> body
                                                       :customerId
                                                       vector
                                                       (conj :prod-mimi))])]
                  (condp = status
                    200 (d/success! d* body)
                    (d/error! d* (ex-info (str "error!" status) {:type :mimi
                                                                 :status status
                                                                 :body body})))))
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

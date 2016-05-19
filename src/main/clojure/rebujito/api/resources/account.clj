(ns rebujito.api.resources.account
  (:refer-clojure :exclude [methods])
  (:require
   [rebujito.protocols :as p]
   [rebujito.api.util :refer :all]
   [cheshire.core :as json]
   [schema.core :as s]
   [yada.resource :refer [resource]]))

(def schema {:post {:countrySubdivision String
                    :registrationSource String
                    :addressLine1 String
                    :password String
                    :emailAddress String
                    :city String
                    :firstName String
                    :birthDay String
                    :birthMonth String
                    :lastName String
                    :receiveStarbucksEmailCommunications String
                    :postalCode String
                    :country String}})

(defn create [store mimi user-store]
  (resource
   (-> {:methods
        {:post {:parameters {:query {:access_token String
                                     :market String
                                     (s/optional-key :locale) String}
                             :body (:post schema)}
                :consumes [{:media-type #{"application/json"}
                            :charset "UTF-8"}]
                :response (fn [ctx]
                            (let [[code res] (p/create-account mimi (get-in ctx [:parameters :body]))]
                              (condp =  code
                                      "111000" (>400 ctx ["Username is already taken" "Account Management Service returns error that user name is already taken"])
                                      "111001" (>400 ctx ["Unknown error occured" "Account Management Service returns error
"])
                                      "111005" (>400 ctx ["Email address too long. Must be 50 characters or less." ""])
                                      "111008" (>400 ctx ["Please supply an email address" "Missing emailAddress attribute."])
                                      "111009" (>400 ctx ["Please supply a registration source" "Missing registration source attribute."])
                                      "111011" (>400 ctx ["Please supply a password" "Missing password attribute"])
                                      "111012" (>400 ctx ["Please supply a market" "Missing market parameter is required"])
                                      "111015" (>400 ctx ["Please supply a last name" "Missing lastName attribute"])
                                      "111016" (>400 ctx ["Please supply a first name" "Missing firstName attribute"])
                                      "111022" (>400 ctx ["Password does not meet complexity requirements" "Account Management Service returns error that password does not meet complexity requirements"])
                                      "111023" (>400 ctx ["No Request supplied" "Create Account Request was malformed."])
                                      "111025" (>400 ctx ["Invalid postalCode. No results were resolved from geolookup" "Return this message when no results are found when resolving address from postal code when market = US"])
                                      "111027" (>400 ctx ["Email address is not unique" "Account Management Service returns error that email address is already taken"])
                                      "111036" (>400 ctx ["Invalid characters (Ø, ø, Ë, ë) specified for first and/or last name." ""])
                                      "111039" (>400 ctx ["Invalid market code or country code." ""])
                                      "111041" (>400 ctx ["Invalid email address" "Email address was malformed"])
                                      "111046" (>400 ctx ["firstName failed profanity check." ""])
                                      "500" (>500 ctx ["An unexpected error occurred processing the request."])
                                      (>201 ctx (p/get-and-insert! user-store (get-in ctx [:parameters :body]))))))}}}


       (merge (common-resource :account))
       (merge access-control))))


(defn get-user [store mimi user-store]
  (resource
   (-> {:methods
        {:get {:parameters {:query {:access_token String
                                     (s/optional-key :select) String
                                     (s/optional-key :ignore) String}}
                :consumes [{:media-type #{"application/json"}
                            :charset "UTF-8"}]
               :response (fn [ctx]
                           (try
                             (if-let [res (p/find user-store (get-in ctx [:parameters :query :access_token]))]
                               (>201 ctx res)
                               (>404 ctx ["Not Found" "Account Profile with given userId was not found."]))
                             (catch Exception e
                               (>500 ctx ["An unexpected error occurred processing the request." (str "caught exception: " (.getMessage e))]))))}}}


       (merge (common-resource :account))
       (merge access-control))))

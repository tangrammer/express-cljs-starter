(ns rebujito.api.resources.card
  (:require
   [rebujito.protocols :as p]
   [rebujito.api.util :refer :all]
   [cheshire.core :as json]
   [schema.core :as s]
   [yada.resource :refer [resource]]))



(defn get-cards [store]
  (resource
   (->
    {:methods
     {:get {:parameters {:query {:access_token String}}
             :consumes [{:media-type #{"application/json" "application/xml"}
                         :charset "UTF-8"}]

             :response (fn [ctx]
                         (condp = (get-in ctx [:parameters :query :access_token])

                           "500" (>500 ctx ["Internal Server Error :( " "An unexpected error occurred processing the request.
"])
                           (>201 ctx [(p/get-card store)])
                           ))}}}
    (merge (common-resource :me/cards))
    (merge access-control))))

(defn register-physical [store]
  (resource
   (->
    {:methods
     {:post {:parameters {:query {:access_token String}
                          :body {:cardNumber String
                                 :pin String}}
             :consumes [{:media-type #{"application/json" "application/xml"}
                         :charset "UTF-8"}]

             :response (fn [ctx]
                         (condp = (get-in ctx [:parameters :query :access_token])
                           "121000" (>400 ctx ["No request supplied." "Request was malformed"])
                           "121001" (>400 ctx ["Please supply a card number." "Missing or invalid 16-digit card number attribute. A valid string of length 16 is required."])
                           "121002" (>400 ctx ["Please supply a pin." "Missing or invalid 8-digit pin attribute is required. A valid string of length 8 is required."])
                           "121016" (>400 ctx ["No registration address on file." "Registration address must already exist for user."])
                           "121017" (>400 ctx ["Cannot register card since card is not valid." "Upon registering a card, First Data Value Link could not resolve card by number and pin"])

                           "121024" (>400 ctx ["Invalid operation for card class." "Service Recovery Card"])
                           "121030" (>400 ctx ["Card is inactive." "Card has never been activated at the POS. Only the in-store POS can activate physical cards because it requires money to be loaded with the act of activation."])

                           "121030" (>403 ctx ["Forbidden" "You have not been granted permission to access the requested method or object."])
                           "403" (>403 ctx ["Forbidden" "You have not been granted permission to access the requested method or object."])
                           "121032" (>403 ctx ["Card is reported lost or stolen." ""])
                           "121037" (>403 ctx ["Card is stolen." ""])
                           "122000" (>403 ctx ["Card is already registered.." "Card number and pin are already registered to user."])

                           "500" (>500 ctx ["Internal Server Error :( "])
                           (>201 ctx (p/get-card store))
                           ))}}}
    (merge (common-resource :me/cards))
    (merge access-control))))

(defn register-digital-cards [store]
  (resource
   (->
    {:methods
     {:post {:parameters {:query {:access_token String}}
             :consumes [{:media-type #{"application/json" "application/xml"}
                         :charset "UTF-8"}]

             :response (fn [ctx]
                         (condp = (get-in ctx [:parameters :query :access_token])
                           "400" (>400 ctx ["No registration address on file. Registration address must already exist for user."])
                           "500" (>500 ctx ["Internal Server Error :( "])
                           (>201 ctx (p/get-card store))
                           ))}}}
    (merge (common-resource :me/cards))
    (merge access-control))))

(ns rebujito.api.resources.card
  (:require
   [manifold.deferred :as d]
   [rebujito.api.resources :refer (domain-exception)]
   [rebujito.protocols :as p]
   [rebujito.api.util :as util]
   [rebujito.mongo :as mongo]
   [cheshire.core :as json]
   [schema.core :as s]
   [yada.resource :refer [resource]]))

(defn get-cards [store]
  (resource
   (->
    {:methods
     {:get {:parameters {:query {:access_token String}}
             :consumes [{:media-type #{"application/json"}
                         :charset "UTF-8"}]
             :response (fn [ctx]
                        ; TODO
                        ;  (>200 ctx [(p/get-cards store)])
                         (util/>200 ctx []))}}}
    (merge (util/common-resource :me/cards))
    (merge util/access-control))))

(defn unregister [store]
  (resource
   (->
    {:methods
     {:delete {:parameters {:path {:card-id String}
                            :query {:access_token String}}
               :consumes [{:media-type #{"application/json"}
                           :charset "UTF-8"}]

               :response (fn [ctx]
                           (condp = (get-in ctx [:parameters :query :access_token])
                             "500"    (util/>500 ctx ["Internal Server Error :( " "An unexpected error occurred processing the request."])
                             "403"    (util/>403 ctx ["Forbidden" "You have not been granted permission to access the requested method or object."])
                             "121032" (util/>403 ctx ["Card is reported lost or stolen" ""])
                             "121037" (util/>403 ctx ["Card is closed." ""])
                             "404"    (util/>404 ctx ["Not Found" "Resource was not found"])
                             "121018" (util/>400 ctx ["Cannot unregister a digital card that has a balance greater than zero." "Only zero balance digital cards can be unregistered"])
                             (util/>200 ctx ["OK" "Success"])))}}}

    (merge (util/common-resource :me/cards))
    (merge util/access-control))))

(def schema {:post {:register-physical {:cardNumber String
                                        :pin String}}})

(defn register-physical [store mimi user-store]
  (resource
   (->
    {:methods
     {:post {:parameters {:query {:access_token String}
                          :body (-> schema :post :register-physical)}
             :consumes [{:media-type #{"application/json"}
                         :charset "UTF-8"}]

             :response (fn [ctx]
                         (let [cardNumber #_(str (+ (rand-int 1000) (read-string (format "96235709%05d" 0))))
                               (get-in ctx [:parameters :body :cardNumber])]
                           (-> (p/register-physical-card mimi {:cardNumber cardNumber
                                                               :customerId  (-> (p/find user-store) last (get "_id") str mongo/id>mimi-id)})

                               (d/chain
                                (fn [mimi-res]
                                  (util/>200 ctx (assoc @(p/get-deferred-card store {}) :cardNumber cardNumber))))
                               (d/catch clojure.lang.ExceptionInfo
                                   (fn [exception-info]
                                     (domain-exception ctx (ex-data  exception-info))))
                               (d/catch Exception
                                   #(util/>500* ctx (str "ERROR CAUGHT!" (.getMessage %))))))

                         #_(condp = (get-in ctx [:parameters :query :access_token])
                             "121000" (>400 ctx ["No request supplied." "Request was malformed"])
                             "121001" (>400 ctx ["Please supply a card number." "Missing or invalid 16-digit card number attribute. A valid string of length 16 is required."])
                             "121002" (>400 ctx ["Please supply a pin." "Missing or invalid 8-digit pin attribute is required. A valid string of length 8 is required."])
                             "121016" (>400 ctx ["No registration address on file." "Registration address must already exist for user."])
                             "121017" (>400 ctx ["Cannot register card since card is not valid." "Upon registering a card, First Data Value Link could not resolve card by number and pin"])

                             "121024" (>400 ctx ["Invalid operation for card class." "Service Recovery Card"])
                             "121030" (>400 ctx ["Card is inactive." "Card has never been activated at the POS. Only the in-store POS can activate physical cards because it requires money to be loaded with the act of activation."])

                             "121030" (>403 ctx ["Forbidden" "You have not been granted permission to access the requested method or object."])
                             "403"    (>403 ctx ["Forbidden" "You have not been granted permission to access the requested method or object."])
                             "121032" (>403 ctx ["Card is reported lost or stolen." ""])
                             "121037" (>403 ctx ["Card is stolen." ""])
                             "122000" (>403 ctx ["Card is already registered.." "Card number and pin are already registered to user."])

                             "500" (>500 ctx ["Internal Server Error :( "])
                             (>200 ctx (p/get-cards store))))}}}

    (merge (util/common-resource :me/cards))
    (merge util/access-control))))

(defn register-digital-cards [store]
  (resource
   (->
    {:methods
     {:post {:parameters {:query {:access_token String}}
             :consumes [{:media-type #{"application/json"}
                         :charset "UTF-8"}]

             :response (fn [ctx]
                         (condp = (get-in ctx [:parameters :query :access_token])
                           "400" (util/>400 ctx ["No registration address on file. Registration address must already exist for user."])
                           "500" (util/>500 ctx ["Internal Server Error :( "])
                           (util/>201 ctx (p/get-cards store))))}}}

    (merge (util/common-resource :me/cards))
    (merge util/access-control))))

(def empty-history {:paging {:total 0
                             :offset 0
                             :limit 10
                             :returned 0}
                    :historyItems []})

(defn history [store]
  (resource
   (->
    {:methods
     {:get {:parameters {:query {:access_token String
                                  (s/optional-key :limit) String
                                  (s/optional-key :offset) String
                                  }}
             :consumes [{:media-type #{"application/json"}
                         :charset "UTF-8"}]

             :response (fn [ctx]
                         (util/>200 ctx empty-history))}}}

    (merge (util/common-resource :me/cards))
    (merge util/access-control))))

(defn execute-payment-deferred [payment-gateway profile-data card-data payment-method-data amount]
  )



(defn reload [store payment-gateway mimi]
  (resource
    (-> {:methods
         {:post {:parameters {:query {:access_token String}
                              :path {:card-id String}
                              :body {:amount Long
                                     :paymentMethodId String
                                     :sessionId String
                                     }}
                 :consumes [{:media-type #{"application/json"}
                             :charset "UTF-8"}]
                 :response (fn [ctx]
                             (->
                              (d/let-flow [profile-data (p/get-deferred-profile store)
                                           card-data (p/get-deferred-card store (-> ctx :parameters :body :card-id))
                                           payment-method-data (p/get-deferred-payment-method-detail store (-> ctx :parameters :body :paymentMethodId))
                                           ]

                               (d/chain
                                (->
                                 (p/execute-payment payment-gateway {:firstName (-> profile-data :user :firstName)
                                                                     :lastName (-> profile-data :user :lastName)
                                                                     :emailAddress (-> profile-data :user :email)
                                                                     :routingNumber (-> payment-method-data :routingNumber)
                                                                     :cvn (-> payment-method-data :cvn)
                                                                     :transactionId "12345"
                                                                     :currency (-> card-data :balanceCurrencyCode)
                                                                     :amount (-> ctx :parameters :body :amount)
                                                                     })
                                 (d/chain
                                     (fn [payment-data]
                                       (p/load-card mimi (-> ctx :parameters :body :card-id) (-> ctx :parameters :body :amount)))
                                     (fn [mimi-card-data]
                                       (util/>200 ctx {:cardId nil
                                                       :balance 416.02
                                                       :balanceDate "2014-03-03T20:17:51.4329837Z"
                                                       :balanceCurrencyCode "ZAR"
                                                       :cardNumber "7777064158671182"}))))))
                              (d/catch clojure.lang.ExceptionInfo
                                  (fn [exception-info]
                                    (domain-exception ctx (ex-data exception-info))))))}}}

        (merge (util/common-resource :me/cards))
        (merge util/access-control))))

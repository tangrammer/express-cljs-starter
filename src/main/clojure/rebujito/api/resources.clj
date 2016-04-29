(ns rebujito.api.resources
  (:require
   [rebujito.protocols :as p]
   [cheshire.core :as json]
   [schema.core :as s]
   [yada.resource :refer [resource]]))

(def access-control
  {:access-control {}
   })

(defn fake [store]
  (resource
   (->
    {:description "fake"
     :swagger/tags ["fake-calls"]

     :produces [{:media-type #{"application/json" "application/xml"}
                 :charset "UTF-8"}]
     :methods
     {:get {:parameters {:path {:id Long}}
             :consumes [{:media-type #{"application/json" "application/xml"}
                         :charset "UTF-8"}]
             :response (fn [ctx]
                         {:id (get-in ctx [:parameters :path :id])
                          :message-for-mom "hi mom"
                          :camelCase :sux})}}}

    (merge access-control))))

(defn register-digital-card [store]
  (resource
   (->
    {:description "register-digital-card"
     :produces [{:media-type
                 #{"application/json" "application/xml"}
                 :charset "UTF-8"}]
     :swagger/tags ["digital-card"]
     :methods
     {:post {:parameters {:query {:access_token String}}
             :consumes [{:media-type #{"application/json" "application/xml"}
                         :charset "UTF-8"}]

             :response (fn [ctx]
                         (condp = (get-in ctx [:parameters :query :access_token])
                           "400" (-> ctx :response (assoc :status 400)
                                     (assoc :body ["No registration address on file. Registration address must already exist for user."])
                                     )
                           "500" (-> ctx :response (assoc :status 500)
                                     (assoc :body ["Internal Server Error :( "]))
                            (-> ctx :response (assoc :status 201)
                                   (assoc :body (p/get-card store)))
                           ))}}}

    (merge access-control))))



(defn get-payment-method [store]
  (resource
   (->
    {:description "get-payment-method"
     :produces [{:media-type
                 #{"application/json" "application/xml"}
                 :charset "UTF-8"}]
     :swagger/tags ["payment-method"]
     :methods
     {:get {:parameters {:path {:payment-mehod-id String}}
             :consumes [{:media-type #{"application/json" "application/xml"}
                         :charset "UTF-8"}]

             :response (fn [ctx]
                         (condp = (get-in ctx [:parameters :path :payment-mehod-id])
                           "404" (-> ctx :response (assoc :status 400)
                                     (assoc :body ["Resource was not found"])
                                     )
                           "500" (-> ctx :response (assoc :status 500)
                                     (assoc :body ["An unexpected error occurred processing the request."]))
                            (-> ctx :response (assoc :status 201)
                                   (assoc :body (p/get-payment-method store)))
                           ))}}}

    (merge access-control))))

(defn post-payment-method [store]
  (resource
   (->
    {:description "post-payment-method"
     :produces [{:media-type
                 #{"application/json" "application/xml"}
                 :charset "UTF-8"}]
     :swagger/tags ["payment-method"]
     :methods
     {:post {:parameters {:query {:access_token String}
                          :body {:expirationYear Long
                                 :billingAddressId String
                                 :accountNumber String
                                 :default String
                                 :nickname String
                                 :paymentType String
                                 :cvn String
                                 :fullName String
                                 :expirationMonth Long}}
             :consumes [{:media-type #{"application/json" "application/xml"}
                         :charset "UTF-8"}]

             :response (fn [ctx]
                         (condp = (get-in ctx [:parameters :query :access_token])
                           "404" (-> ctx :response (assoc :status 400)
                                     (assoc :body ["Resource was not found"])
                                     )
                           "500" (-> ctx :response (assoc :status 500)
                                     (assoc :body ["An unexpected error occurred processing the request."]))
                            (-> ctx :response (assoc :status 201)
                                   (assoc :body (p/post-payment-method store)))
                           ))}}}

    (merge access-control))))

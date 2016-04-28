(ns rebujito.resources
  (:require
   [clojure.tools.logging :refer :all]
   [clojure.java.io :as io]
   [rebujito.mocks :as mocks]
   [rebujito.protocols :as p]
   [cheshire.core :as json]
   [schema.core :as s]
   [yada.methods :as m]
   [yada.resource :refer [resource]]
   [yada.yada :as yada])
  (:import [manifold.stream.core IEventSource]))

(def access-control
  {:access-control
   {:realm "swarmloyalty"
    :authentication-schemes
    [{:scheme "Basic"
      :verify {["tom" "watson"] {:email "tom@ibm.com"
                                 :roles #{:phonebook/write
                                          :phonebook/delete}}
               ["malcolm" "changeme"] {:email "malcolm@juxt.pro"
                                       :roles #{:phonebook/write}}}}

     ;; A custom scheme (indicated by the absence of a :scheme entry) that lets us process api-keys.
     ;; You can plugin your own verifier here, with full access to yada's request context.
     ;; This verifier is just a simple example to allow the Swagger UI to access the phonebook.
     {:verify
      (fn [ctx]
        (let [k (get-in ctx [:request :headers "Api-Key"])]
          (cond
            (= k "masterkey") {:user "swagger-master"
                               :roles #{:phonebook/write :phonebook/delete}}
            (= k "lesserkey") {:user "swagger-lesser"
                               :roles #{:phonebook/write}}
            k {})))}]

    :authorization
    {:roles/methods
     {:get true
      :post :phonebook/write
      :put :phonebook/write
      :delete :phonebook/delete
      ;; TODO: Write a thing where we can have multiple keys
      ;; TODO: Maybe coerce it!
      ;; #{:post :put :delete} :phonebook/write
      }}

    ;; Access to our phonebook is public, but if we want to modify it we
    ;; must have sufficient authorization. This is what this access
    ;; control definition does.

    ;; We must be very careful not to allow a rogue script on another
    ;; website to hijack our cookies and destroy our phonebook!.

    ;; We want to allow read-access to our phonebook generally
    ;; available to foreign applications (those originating from
    ;; different hosts).
    :allow-origin "*"

    ;; Only allow origins we know about write-access, by restricting
    ;; our mutable methods
    :allow-methods (fn [ctx]
                     ;; If same origin, or origin is our swagger ui,
                     ;; we'll allow the unsafe methods
                     (if (#{"http://localhost:8090"
                            "https://yada.juxt.pro"
                            "http://yada.juxt.pro.local"
                            (yada/get-host-origin (:request ctx))}
                          (get-in ctx [:request :headers "origin"]))
                       #{:get :post :put :delete}
                       #{:get}))

    ;; It's a feature of our restricted write-access policy that we don't need to
    ;; authenticate users from other origins.
    :allow-credentials false

    ;; Required for the Swagger key
    :allow-headers ["Api-Key"]
    }})

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

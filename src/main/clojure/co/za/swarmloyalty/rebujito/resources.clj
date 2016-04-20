(ns co.za.swarmloyalty.rebujito.resources
  (:require
   [clojure.tools.logging :refer :all]
   [clojure.java.io :as io]
   [co.za.swarmloyalty.rebujito.mocks :as mocks]
   [cheshire.core :as json]
   [schema.core :as s]
   [yada.methods :as m]
   [yada.protocols :as p]
   [yada.resource :refer [resource]]
   [yada.yada :as yada])
  (:import [manifold.stream.core IEventSource]))

(def access-control
  {:access-control
   {:realm "phonebook"
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

(defn register-digital-card []
  (resource
   (->
    {:description "register-digital-card"
     :produces [{:media-type
                 #{"application/json" "application/xml"}
                 :charset "UTF-8"}]
     :methods
     {:post {:parameters {:query {:access_token String}}
             :consumes [{:media-type #{"application/json" "application/xml"}
                         :charset "UTF-8"}]
             :response (fn [ctx]
                         mocks/card
                         )}}}

    (merge access-control))))

(defn new-entry-resource []
  (resource
   (->
    {:description "Phonebook entry"
     :parameters {:path {:entry Long}}
     :produces [{:media-type #{"text/html"
                               "application/edn;q=0.9"
                               "application/json;q=0.8"}
                 :charset "UTF-8"}]

     :methods
     {:get
      {:swagger/tags ["default" "getters"]
       :response
       (fn [ctx]

         (let [id (get-in ctx [:parameters :path :entry])
               {:keys [firstname surname phone] :as entry} {:firstname "example"
                                                            :surname "example"
                                                            :phone "example"}]
           (when entry
             (case (yada/content-type ctx)
               "text/html"
               "returning html"
               entry))))}

      :put
      {:parameters
       {:form {:surname String
               :firstname String
               :phone String}}

       :consumes
       [{:media-type #{"multipart/form-data"
                       "application/x-www-form-urlencoded"}}]

       :response
       (fn [ctx]
         (let [entry (get-in ctx [:parameters :path :entry])
               form (get-in ctx [:parameters :form])]
           (assert entry)
           (assert form)
           "db/updating entity"))}

      :delete
      {:produces "text/plain"
       :response
       (fn [ctx]
         (let [id (get-in ctx [:parameters :path :entry])]
           "db/removing entity"
           (let [msg (format "Entry %s has been removed" id)]
             (case (get-in ctx [:response :produces :media-type :name])
               "text/plain" (str msg "\n")
               "text/html" "more html"
               ;; We need to support JSON for the Swagger UI
               {:message msg}))))}}}

    (merge access-control))))

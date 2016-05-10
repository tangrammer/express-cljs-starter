(ns starbucks.resources
  (:require
   [clojure.tools.logging :as log]
   [clojure.java.io :as io]
   [hiccup.core :refer [html]]
   [schema.core :as s]
   [starbucks.db :as db]
   [starbucks.schema :as sch]
   [starbucks.html :as html]
   [starbucks.oauth :as auth]
   [yada.methods :as m]
   [yada.protocols :as p]
   [yada.resource :refer [resource]]
   [yada.yada :as yada])
  (:import [manifold.stream.core IEventSource]))

(def access-control
  {:access-control
   {:realm "starbucks"
    :authentication-schemes
    [{:scheme "Basic"
      :verify {["john" "doe"] {:email "john.doe@test.org"
                                 :roles #{:starbucks/write
                                          :starbucks/delete}}
               ["dev" "test"] {:email "dev.test@test.org"
                                       :roles #{:starbucks/write}}}}

     ;; A custom scheme (indicated by the absence of a :scheme entry) that lets us process api-keys.
     ;; You can plugin your own verifier here, with full access to yada's request context.
     ;; This verifier is just a simple example to allow the Swagger UI to access the starbucks.
     {:verify
      (fn [ctx]
        (let [k (get-in ctx [:request :headers "Api-Key"])]
          (cond
            (= k "masterkey") {:user "swagger-master"
                               :roles #{:starbucks/write :starbucks/delete}}
            (= k "lesserkey") {:user "swagger-lesser"
                               :roles #{:starbucks/write}}
            k {})))}]

    :authorization
    {:roles/methods
     {:get true
      :post :starbucks/write
      :put :starbucks/write
      :delete :starbucks/delete}}
      ;; TODO: Write a thing where we can have multiple keys
      ;; TODO: Maybe coerce it!
      ;; #{:post :put :delete} :starbucks/write


    ;; We want to allow read-access to our starbucks generally
    ;; available to foreign applications (those originating from
    ;; different hosts).
    :allow-origin "*"

    ;; Only allow origins we know about write-access, by restricting
    ;; our mutable methods
    :allow-methods (fn [ctx]
                     ;; If same origin, or origin is our swagger ui,
                     ;; we'll allow the unsafe methods
                     (if (#{"http://localhost:8090"
                            (yada/get-host-origin (:request ctx))}
                          (get-in ctx [:request :headers "origin"]))
                       #{:get :post :put :delete}
                       #{:get}))

    ;; It's a feature of our restricted write-access policy that we don't need to
    ;; authenticate users from other origins.
    :allow-credentials false

    ;; Required for the Swagger key
    :allow-headers ["Api-Key"]}})


(defn new-index-resource [db]
  (resource
   (->
    {:description "Starbucks entries"
     :produces [{:media-type
                 #{"text/html" "application/edn;q=0.9" "application/json;q=0.8"}
                 :charset "UTF-8"}]
     :methods
     {:get {:parameters {:query {(s/optional-key :q) String}}
            :swagger/tags ["default" "getters"]
            :response (fn [ctx]
                        (let [q (get-in ctx [:parameters :query :q])
                              entries (if q
                                        (db/search-entries db q)
                                        (db/get-entries db))]
                          (case (yada/content-type ctx)
                            "text/html" (html/index-html ctx entries q)
                            entries)))}

      :post {:parameters {:form {:username String :userid String :password String}}
             :consumes [{:media-type #{"application/x-www-form-urlencoded"}
                         :charset "UTF-8"}]
             :response (fn [ctx]
                         (let [id (db/add-entry db (get-in ctx [:parameters :form]))]
                           (java.net.URI. (:uri (yada/uri-for ctx :starbucks.api/entry {:route-params {:entry id}})))))}}}
    (merge access-control))))

(defn new-entry-resource [db]
  (resource
   (->
    {:description "Starbucks entry"
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
               {:keys [username userid password] :as entry} (db/get-entry db id)]
           (when entry
             (case (yada/content-type ctx)
               "text/html"
               (html/entry-html
                entry
                {:entry (:path (yada/uri-for ctx :starbucks.api/entry {:route-params {:entry id}}))
                 :index (:path (yada/uri-for ctx :starbucks.api/index))})
               entry))))}

      :put
      {:parameters
       {:form {:username String
               :userid String
               :password String}}

       :consumes
       [{:media-type #{"multipart/form-data"
                       "application/x-www-form-urlencoded"}}]

       :response
       (fn [ctx]
         (let [entry (get-in ctx [:parameters :path :entry])
               form (get-in ctx [:parameters :form])]
           (assert entry)
           (assert form)
           (db/update-entry db entry form)))}

      :delete
      {:produces "text/plain"
       :response
       (fn [ctx]
         (let [id (get-in ctx [:parameters :path :entry])]
           (db/delete-entry db id)
           (let [msg (format "Entry %s has been removed" id)]
             (case (get-in ctx [:response :produces :media-type :name])
               "text/plain" (str msg "\n")
               "text/html" (html [:h2 msg])
               ;; We need to support JSON for the Swagger UI
               {:message msg}))))}}}
    (merge access-control))))

(defn new-oauth-resource [db]
  (resource
    {:description "Starbucks oauth2"
     :produces [{:media-type #{"application/json"}
                 :charset "UTF-8"}]
     :methods
     {:post {:parameters {:body sch/TokenRequest
                          :query {:sig String}}
             :consumes [{:media-type #{"application/json"}
                         :charset "UTF-8"}]
             :response auth/get-token-response}}}))

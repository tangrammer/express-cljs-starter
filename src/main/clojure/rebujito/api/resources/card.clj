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

(ns rebujito.api.resources
  (:require
   [rebujito.protocols :as p]
   [rebujito.api.util :refer :all]
   [cheshire.core :as json]
   [schema.core :as s]
   [yada.resource :refer [resource]]))



(defn register-digital-card [store]
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
    (merge (common-resource :me/register-digital-card))
    (merge access-control))))

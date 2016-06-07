(ns rebujito.api.resources.addresses
  (:require
   [rebujito.protocols :as p]
   [rebujito.scopes :as scopes]
   [rebujito.api.util :as util]
   [cheshire.core :as json]
   [monger.operators :refer [$push]]
   [schema.core :as s]
   [yada.resource :refer [resource]]))

(def schema {:post {:addressLine1 String
                    :city String
                    :firstName String
                    :type String
                    :addressLine2 String
                    :lastName String
                    :postalCode String
                    :phoneNumber String
                    :country String}})

(defn insert-address [user-store user address]
  (p/update-by-id! user-store (:_id user) {$push {:addresses address}}))

(defn create [user-store authorizer authenticator]
  (resource
   (-> {:methods
        {:post {:parameters {:query {:access_token String}
                             :body (:post schema)}
                :consumes [{:media-type #{"application/json"}
                            :charset "UTF-8"}]
                :response (fn [ctx]
                            (insert-address [user-store (org.bson.types.ObjectId. "0000000000000000028f0cc5") (-> ctx :request :body)])
                            (-> ctx :response (assoc :status 201)
                                (assoc :body ["created"])
                                (assoc-in [:headers :location] "/me/addresses/TODO_id1")))}}}


       (merge (util/common-resource :addresses))
       (merge {:access-control {}} #_(util/access-control*  authenticator authorizer {:get scopes/user}
               )))))

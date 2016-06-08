(ns rebujito.api.resources.addresses
  (:require
   [rebujito.protocols :as p]
   [rebujito.scopes :as scopes]
   [rebujito.api.util :as util]
   [cheshire.core :as json]
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

(defn create [user-store authorizer authenticator]
  (-> {:methods
       {:post {:parameters {:query {:access_token String}
                            :body (:post schema)}
               :consumes [{:media-type #{"application/json"}
                           :charset "UTF-8"}]
               :response (fn [ctx]
                           (-> ctx :response (assoc :status 201)
                               (assoc :body ["created"])
                               (assoc-in [:headers :location] "/balabas")))}}}


      (merge (util/common-resource :addresses))
      (merge {:access-control {}} #_(util/access-control*  authenticator authorizer {:get scopes/user}
                                                           ))))

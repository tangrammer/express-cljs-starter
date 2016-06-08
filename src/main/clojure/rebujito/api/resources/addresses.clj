(ns rebujito.api.resources.addresses
  (:require
   [rebujito.protocols :as p]
   [rebujito.scopes :as scopes]
   [rebujito.api.util :as util]
   [rebujito.api.resources :refer (domain-exception)]
   [cheshire.core :as json]
   [manifold.deferred :as d]
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

(defn insert-address [user-store user-id address]
  (let [uuid (str (java.util.UUID/randomUUID))
        address (assoc address :uuid uuid)]
    (p/update-by-id! user-store user-id {$push {:addresses address}})
    uuid))

(defn create [user-store]
  (-> {:methods
       {:post {:parameters {:query {:access_token String}
                            :body (:post schema)}
               :response (fn [ctx]
                           (-> (d/let-flow [auth-user (util/authenticated-user ctx)
                                            address (-> ctx :body)
                                            uuid (insert-address user-store (:_id auth-user) address)]
                                (-> ctx :response (assoc :status 201)
                                    ; TODO use ::resource/addresses to generate :location
                                    (assoc-in [:headers :location] (str "/me/addresses/" uuid))))
                               (d/catch clojure.lang.ExceptionInfo
                                   (fn [exception-info]
                                     (domain-exception ctx (ex-data exception-info))))))}
        :get {:parameters {:query {:access_token String}}
              :response (fn [ctx]
                          (-> (d/let-flow [auth-user (util/authenticated-user ctx)
                                           user-id (:_id auth-user)
                                           addresses (:addresses (p/find user-store user-id))]
                               (util/>200 ctx addresses))))}}}
      (merge (util/common-resource :addresses))))

(defn get-one [user-store]
  (-> {:methods
       {:get {:parameters {:path {:address-uuid String}
                           :query {:access_token String}}
              :response (fn [ctx]
                          (-> (d/let-flow [auth-user (util/authenticated-user ctx)
                                           address-uuid (-> ctx :parameters :path :address-uuid)
                                           user-id (:_id auth-user)
                                           addresses (:addresses (p/find user-store user-id))]
                               (util/>200 ctx (some #(and (= address-uuid (:uuid %)) %) addresses)))))}}}
     (merge (util/common-resource :addresses))))

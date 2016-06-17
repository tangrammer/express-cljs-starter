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



(defn addresses [user-store]
  (-> {:methods
       {:post {:parameters {:query {:access_token String}
                            :body (:post schema)}
               :response (fn [ctx]
                           (-> (d/let-flow [auth-user (util/authenticated-user ctx)
                                            address (-> ctx :body)
                                            address-id (p/insert-address user-store (:_id auth-user) address)]
                                (-> ctx :response (assoc :status 201)
                                    ; TODO use ::resource/addresses to generate :location
                                    (assoc-in [:headers :location] (str "/me/addresses/" address-id))))
                               (d/catch clojure.lang.ExceptionInfo
                                   (fn [exception-info]
                                     (domain-exception ctx (ex-data exception-info))))))}
        :get {:parameters {:query {:access_token String}}
              :response (fn [ctx]
                          (-> (d/let-flow [auth-user (util/authenticated-user ctx)
                                           user-id (:_id auth-user)
                                           addresses (p/get-addresses user-store user-id)]
                               (util/>200 ctx addresses))))}}}
      (merge (util/common-resource :addresses))))

(defn get-one [user-store]
  (-> {:methods
       {:get {:parameters {:path {:address-id String}
                           :query {:access_token String}}
              :response (fn [ctx]
                          (-> (d/let-flow [auth-user (util/authenticated-user ctx)
                                           address-id (-> ctx :parameters :path :address-id)
                                           user-id (:_id auth-user)
                                           addresses (:addresses (p/find user-store user-id))]
                                          (util/>200 ctx (some #(and (= address-id (:addressId %)) %) addresses)))
                              (d/catch clojure.lang.ExceptionInfo
                                  (fn [exception-info]
                                    (domain-exception ctx (ex-data exception-info))))))}

        :delete {:parameters {:query {:access_token String}
                              :path {:address-id String}}
                 :response (fn [ctx]
                             ;; TODO: complete possible outputs
                             ;;      400	111028	Cannot delete registration address.
                             ;;      400	111037	Address is in use.
                             #_(-> (d/let-flow [auth-user (util/authenticated-user ctx)

                                              res-delete (p/delete-card-token payment-gateway {:cardToken (-> payment-method :routingNumber)})
                                              res-mongo (when res-delete
                                                          (p/remove-payment-method user-store (:_id auth-user) payment-method))]


                                             (util/>200 ctx ["OK" "Success" res-mongo]))
                                 (d/catch clojure.lang.ExceptionInfo
                                     (fn [exception-info]
                                       (domain-exception ctx (ex-data exception-info))))))}

        }}

     (merge (util/common-resource :addresses))))

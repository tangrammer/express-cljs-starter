(ns rebujito.api.resources.addresses
  (:require
   [taoensso.timbre :as log]
   [rebujito.protocols :as p]
   [rebujito.scopes :as scopes]
   [rebujito.api.util :as util]
   [rebujito.util :refer (dcatch)]
   [rebujito.api.resources :refer (domain-exception)]
   [cheshire.core :as json]
   [manifold.deferred :as d]
   [monger.operators :refer [$push]]
   [schema.core :as s]
   [yada.resource :refer [resource]]))

(def schema {:post {:addressLine1 String
                    (s/optional-key :addressLine2) String
                    :city String
                    :country String
                    :firstName String
                    :lastName String
                    :phoneNumber String
                    :postalCode String
                    :type String}

             :put {:addressLine1 String
                   (s/optional-key :addressLine2) String
                   :city String
                   :country String
                   :firstName String
                   :lastName String
                   :phoneNumber String
                   :postalCode String
                   :type String
                   (s/optional-key :name) String
                   (s/optional-key :countrySubdivision) String
                   (s/optional-key :addressId) String}

             :response {:post {:addressLine1 String
                               (s/optional-key :addressLine2) String
                               :city String
                               :country String
                               :firstName String
                               :lastName String
                               :phoneNumber String
                               :postalCode String
                               :type String}}})


(defn update-address* [ctx payload user-id address-id user-store]
  (d/let-flow [
               new-address (assoc payload :addressId address-id)
               updated? (p/update-address user-store user-id new-address)]
              (util/>200 ctx (when updated? nil))))

(defn addresses [user-store]
  (-> {:methods
       {:post {:parameters {:query {:access_token String}
                            :body (:post schema)}
               :response (fn [ctx]
                           (dcatch  ctx
                                    (d/let-flow [auth-data (util/authenticated-data ctx)
                                                 address (-> ctx :body)
                                                 address-id (p/insert-address user-store (:user-id auth-data) address)]
                                                (log/info ">====>>>"(str "/me/addresses/" address-id))
                                                (-> ctx :response (assoc :status 201)
                                                    ;; TODO use ::resource/addresses to generate :location
                                                    (assoc-in [:headers :location] (str "/me/addresses/" address-id))))
))}
        :get {:parameters {:query {:access_token String}}
              :response (fn [ctx]
                          (dcatch ctx (d/let-flow [auth-data (util/authenticated-data ctx)
                                           user-id (:user-id auth-data)
                                           addresses (p/get-addresses user-store user-id)]
                               (util/>200 ctx addresses))))}}}
      (merge (util/common-resource :addresses))))

(defn get-one [user-store]
  (-> {:methods
       {:get {:parameters {:path {:address-id String}
                           :query {:access_token String}}
              :response (fn [ctx]
                          (dcatch ctx (d/let-flow [auth-data (util/authenticated-data ctx)
                                           user-id (:user-id auth-data)
                                           address-id (-> ctx :parameters :path :address-id)
                                           address (p/get-address user-store user-id address-id)
                                           ]
                                          (util/>200 ctx address))))}

        :delete {:parameters {:query {:access_token String}
                              :path {:address-id String}}
                 :response (fn [ctx]
                             ;; TODO: complete possible outputs
                             ;;      400	111028	Cannot delete registration address.
                             ;;      400	111037	Address is in use.
                             (dcatch ctx
                                     (d/let-flow [auth-data (util/authenticated-data ctx)
                                                  user-id (:user-id auth-data)
                                                  address-id (-> ctx :parameters :path :address-id)
                                                  address (p/get-address user-store user-id address-id)
                                                  res-mongo (p/remove-address user-store user-id address)]

                                                 (util/>200 ctx nil))))}
        :put {:parameters {:query {:access_token String}
                           :path {:address-id String}
                           :body (-> schema :put)}
              :response (fn [ctx]
                          (dcatch ctx
                                  (let [payload (-> ctx :parameters :body)
                                        user-id (-> ctx util/authenticated-data :user-id)
                                        address-id (get-in ctx [:parameters :path :address-id])]
                                    (update-address* ctx payload user-id address-id user-store))))}
        }}

     (merge (util/common-resource :addresses))))

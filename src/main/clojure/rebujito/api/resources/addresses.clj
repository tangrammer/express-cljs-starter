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
                    :addressLine2 String
                    :city String
                    :country String
                    :firstName String
                    :lastName String
                    :phoneNumber String
                    :postalCode String
                    :type String}

             :put {:addressLine1 String
                   :addressLine2 String
                   :city String
                   :country String
                   :firstName String
                   :lastName String
                   :name String
                   :phoneNumber String
                   :postalCode String
                   :type String
                   (s/optional-key :countrySubdivision) String
                   (s/optional-key :addressId) String}

             :response {:post {:addressLine1 String
                               :addressLine2 String
                               :city String
                               :country String
                               :firstName String
                               :lastName String
                               :phoneNumber String
                               :postalCode String
                               :type String}}})

(defn addresses [user-store]
  (-> {:methods
       {:post {:parameters {:query {:access_token String}
                            :body (:post schema)}
               :response (fn [ctx]
                           (dcatch  ctx
                                    (d/let-flow [auth-user (util/authenticated-user ctx)
                                                 address (-> ctx :body)
                                                 address-id (p/insert-address user-store (:user-id auth-user) address)]
                                                (log/info ">====>>>"(str "/me/addresses/" address-id))
                                                (-> ctx :response (assoc :status 201)
                                                    ;; TODO use ::resource/addresses to generate :location
                                                    (assoc-in [:headers :location] (str "/me/addresses/" address-id))))
))}
        :get {:parameters {:query {:access_token String}}
              :response (fn [ctx]
                          (-> (d/let-flow [auth-user (util/authenticated-user ctx)
                                           user-id (:user-id auth-user)
                                           addresses (p/get-addresses user-store user-id)]
                               (util/>200 ctx addresses))))}}}
      (merge (util/common-resource :addresses))))

(defn get-one [user-store]
  (-> {:methods
       {:get {:parameters {:path {:address-id String}
                           :query {:access_token String}}
              :response (fn [ctx]
                          (-> (d/let-flow [auth-user (util/authenticated-user ctx)
                                           user-id (:user-id auth-user)
                                           address-id (-> ctx :parameters :path :address-id)
                                           address (p/get-address user-store user-id address-id)
                                           ]
                                          (util/>200 ctx address))
                              (d/catch clojure.lang.ExceptionInfo
                                  (fn [exception-info]
                                    (domain-exception ctx (ex-data exception-info))))))}

        :delete {:parameters {:query {:access_token String}
                              :path {:address-id String}}
                 :response (fn [ctx]
                             ;; TODO: complete possible outputs
                             ;;      400	111028	Cannot delete registration address.
                             ;;      400	111037	Address is in use.
                             (-> (d/let-flow [auth-user (util/authenticated-user ctx)
                                              user-id (:user-id auth-user)
                                              address-id (-> ctx :parameters :path :address-id)
                                              address (p/get-address user-store user-id address-id)
                                              res-mongo (p/remove-address user-store user-id address)]

                                             (util/>200 ctx nil))
                                 (d/catch clojure.lang.ExceptionInfo
                                     (fn [exception-info]
                                       (domain-exception ctx (ex-data exception-info))))))}
        :put {:parameters {:query {:access_token String}
                           :path {:address-id String}
                           :body (-> schema :put)}
           :response (fn [ctx]
                       (let [payload (-> ctx :parameters :body)]
                         (-> (d/let-flow [user-id (-> ctx util/authenticated-user :user-id)
                                          address-id (get-in ctx [:parameters :path :address-id])
                                          new-address (assoc payload :addressId address-id)]

                               (p/update-address user-store user-id new-address)
                               (util/>200 ctx nil))
                             (d/catch clojure.lang.ExceptionInfo
                                 (fn [exception-info]
                                   (domain-exception ctx (ex-data exception-info)))))))}
        }}

     (merge (util/common-resource :addresses))))

(ns rebujito.api.resources.profile
  (:require
   [manifold.deferred :as d]
   [rebujito.protocols :as p]
   [rebujito.scopes :as scopes]
   [rebujito.api.util :as util]
   [rebujito.api.resources :refer (domain-exception)]
   [cheshire.core :as json]
   [schema.core :as s]
   [yada.resource :refer [resource]]))

(def response-defaults {:socialProfile {}
                        :paymentMethods []
                        :favoriteStores []
                        :devices []
                        :addresses []
                        :rewardsSummary {}
                        :tippingPreferences {}
                        :starbucksCards []})

(def schema {:put {:accountImageUrl String}})

(defn profile [store mimi user-store  app-config]
  (-> {:methods
       {:get {:parameters {:query {:access_token String
                                   (s/optional-key :select) String
                                   (s/optional-key :ignore) String}}
              :response (fn [ctx]
                          (-> (d/let-flow [auth-user (util/authenticated-user ctx)
                                           user-id (:_id auth-user)
                                           user-data (util/generate-user-data auth-user (:sub-market app-config))
                                           real-user-data (p/find user-store user-id)]
                                          (util/>200 ctx (-> response-defaults
                                                             (merge
                                                               {:user user-data}
                                                               (select-keys real-user-data [:addresses :socialProfile])
                                                             )
                                                            ;  !! wrong format !! (merge {:rewardsSummary @(p/rewards mimi {})})
                                                             (dissoc :target-environment))))
                              (d/catch clojure.lang.ExceptionInfo
                                  (fn [exception-info]
                                    (domain-exception ctx (ex-data exception-info))))
                              ))}}}

      (merge (util/common-resource :profile))))

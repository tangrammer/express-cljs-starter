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

(def response-overrides {:rewardsSummary {}
                         :addresses []
                         :socialProfile {}
                         :paymentMethods []
                         :favoriteStores []
                         :devices []
                         :tippingPreferences {}
                         :starbucksCards []})

(def schema {:put {:accountImageUrl String}})

(defn me [store mimi user-store  app-config]
  (-> {:methods
       {:get {:parameters {:query {:access_token String
                                   (s/optional-key :select) String
                                   (s/optional-key :ignore) String}}
              :response (fn [ctx]
                          (-> (d/let-flow [auth-user (util/authenticated-user ctx)
                                           user-data (util/generate-user-data auth-user (:sub-market app-config))
                                           profile-data (p/get-profile store) ]
                                          (util/>200 ctx (-> profile-data
                                                             (merge {:user user-data})
                                                             (merge {:rewardsSummary @(p/rewards mimi {})})
                                                             (merge response-overrides)
                                                             (dissoc :target-environment))))
                              (d/catch clojure.lang.ExceptionInfo
                                  (fn [exception-info]
                                    (domain-exception ctx (ex-data exception-info))))
                              ))}}}


      (merge (util/common-resource :profile))))

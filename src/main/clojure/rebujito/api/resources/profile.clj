(ns rebujito.api.resources.profile
  (:require
   [rebujito.protocols :as p]
   [rebujito.scopes :as scopes]
   [rebujito.api.util :as util]
   [cheshire.core :as json]
   [schema.core :as s]
   [yada.resource :refer [resource]]))

(def response {
               :rewardsSummary {}
               :addresses []
               :socialProfile {}
               :paymentMethods []
               :favoriteStores []
               :devices []
               :tippingPreferences {}
               :starbucksCards []})
              ; dissoc :target-environment))

(def schema {:put {:accountImageUrl String}})

(defn me [store mimi user-store authorizer authenticator]
  (resource
   (-> {:methods
        {:get {:parameters {:query {:access_token String
                                    (s/optional-key :select) String
                                    (s/optional-key :ignore) String}}
                :consumes [{:media-type #{"application/json"}
                            :charset "UTF-8"}]
               :response (fn [ctx]
                           (try
                             (let [auth-user (util/authenticated-user ctx)
                                   user-data (util/generate-user-data auth-user)
                                   profile-data (-> (p/get-profile store)
                                                    (merge {:user user-data})
                                                    (merge {:rewardsSummary @(p/rewards mimi {})}))]
                               (util/>200 ctx profile-data))
                             (catch Exception e
                               (util/>500 ctx ["An unexpected error occurred processing the request." (str "caught exception: " (.getMessage e))]))))}}}


       (merge (util/common-resource :profile))
       (merge (util/access-control* authenticator authorizer {:get scopes/user})))))

(ns rebujito.api.resources.profile
  (:require
   [manifold.deferred :as d]
   [rebujito.protocols :as p]
   [rebujito.scopes :as scopes]
   [rebujito.api.util :as util]
   [rebujito.api.resources :refer (domain-exception)]
   [rebujito.api.resources.rewards :as rewards]
   [rebujito.api.resources.card :as card]
   [rebujito.api.resources.payment :as payment]
   [cheshire.core :as json]
   [schema.core :as s]
   [yada.resource :refer [resource]]))

(def response-defaults {:favoriteStores []
                        :devices []
                        :tippingPreferences {}})

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
                                           real-user-data (p/find user-store user-id)
                                           card-number (-> real-user-data :cards first :cardNumber)
                                           rewards (rewards/rewards-response mimi card-number)
                                           card (card/get-card user-store user-id mimi)
                                           payment-methods (->> (p/get-payment-methods user-store (:_id auth-user))
                                                                (map payment/adapt-mongo-to-spec))]

                                          (util/>200 ctx (-> response-defaults
                                                             (merge
                                                               {:user user-data
                                                                :rewardsSummary rewards
                                                                :paymentMethods payment-methods
                                                                :starbucksCards [card]}
                                                               (select-keys real-user-data [:addresses :socialProfile]))
                                                             (dissoc :target-environment))))
                              (d/catch clojure.lang.ExceptionInfo
                                  (fn [exception-info]
                                    (domain-exception ctx (ex-data exception-info))))))}}}


      (merge (util/common-resource :profile))))

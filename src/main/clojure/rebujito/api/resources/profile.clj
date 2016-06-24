(ns rebujito.api.resources.profile
  (:require
   [manifold.deferred :as d]
   [rebujito.protocols :as p]
   [rebujito.scopes :as scopes]
   [rebujito.api.util :as util]
   [rebujito.util :refer (dcatch error*)]
   [rebujito.api.resources :refer (domain-exception)]
   [rebujito.api.resources.rewards :as rewards]
   [rebujito.api.resources.card :as card]
   [rebujito.api.resources.payment :as payment]
   [cheshire.core :as json]
   [schema.core :as s]
   [yada.resource :refer [resource]]))

(def response-defaults {:favoriteStores []
                        :devices []
                        :addresses []
                        :socialProfile {}
                        :tippingPreferences {}})

(def schema {:put {:accountImageUrl String}})

(defn profile [store mimi user-store  app-config]
  (-> {:methods
       {:get {:parameters {:query {:access_token String
                                   (s/optional-key :select) String
                                   (s/optional-key :ignore) String}}
              :response (fn [ctx]
                          (let [try-id ::profile
                                try-type :api]
                            (dcatch ctx
                                    (d/let-flow [auth-user (util/authenticated-user ctx)
                                                 user-id (:_id auth-user)
                                                 user-data (util/generate-user-data auth-user (:sub-market app-config))
                                                 real-user-data (p/find user-store user-id)
                                                 card-number  (let [try-context '[user-data real-user-data]]
                                                                (or (-> real-user-data :cards first :cardNumber)
                                                                    #_(error* 500 [500 ::card-number-cant-be-null])))

                                                 balances (when (some? card-number)
                                                            (p/balances mimi card-number))
                                                 rewards (rewards/rewards-response balances card-number)
                                                 card (card/get-card* user-store user-id balances)
                                                 payment-methods (->> (p/get-payment-methods user-store (:_id auth-user))
                                                                      (map payment/adapt-mongo-to-spec))]

                                                (util/>200 ctx (-> response-defaults
                                                                   (merge
                                                                    {:user (merge
                                                                            (select-keys user-data [:firstName :lastName :emailAddress])
                                                                            {:email (:emailAddress user-data)}
                                                                            {:exId nil
                                                                             :subMarket "ZA"
                                                                             :partner false})
                                                                     :rewardsSummary rewards
                                                                     :paymentMethods payment-methods
                                                                     :starbucksCards (if card
                                                                                       [card]
                                                                                       [])
                                                                     }
                                                                    (select-keys real-user-data [:addresses :socialProfile]))
                                                                   (dissoc :target-environment)))))))}}}


      (merge (util/common-resource :profile))))

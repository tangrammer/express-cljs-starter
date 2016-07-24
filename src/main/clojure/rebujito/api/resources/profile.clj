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
   [rebujito.api.resources.account :as account]
   [rebujito.schemas :refer (UpdateMongoUser)]
   [cheshire.core :as json]
   [schema.core :as s]
   [plumbing.core :refer [?>]]
   [yada.resource :refer [resource]]))

(def response-defaults {:favoriteStores []
                        :devices []
                        :addresses []
                        :socialProfile {}
                        :tippingPreferences {}})

(defn update-user [ctx user-id user-store mimi]
  (d/let-flow [payload (util/remove-nils (-> ctx :parameters :body))
               payload (-> payload
                           (?> (:birthDay payload) (assoc :birthDay (Integer. (:birthDay payload))))
                           (?> (:birthMonth payload) (assoc :birthMonth (Integer. (:birthMonth payload)))))
               current-user (p/find user-store user-id)
               email-exists? (when (and (:emailAddress payload) (not= (:emailAddress payload) (:emailAddress current-user)))
                               (account/check-account-mongo {:emailAddress (:emailAddress payload)} user-store))

               user (when-not email-exists?
                      (p/find user-store user-id))

               mimi-res (when (and user
                                   (some #(and (some? %) (not= "" %))  (vals payload)))
                          (p/update-account mimi user))
               updated? (when mimi-res
                          (pos? (.getN (p/update-by-id! user-store user-id payload))))
               ]

              (if (or updated? (nil? mimi-res))
                (util/>200 ctx nil)
                (util/>500 ctx "we couldn't update the account")))
  )

(defn load-profile [ctx mimi user-store user-id]
 (d/let-flow [user-data (p/find user-store user-id)
              card-number  (let [try-context '[user-id user-data]]
                             (or (-> user-data :cards first :cardNumber)
                                 #_(error* 500 [500 ::card-number-cant-be-null])))

              balances (when (some? card-number)
                         (p/balances mimi card-number))
              rewards (rewards/rewards-response balances card-number)
              card (card/>get-card user-store (:_id user-data) balances)
              payment-methods (->> (p/get-payment-methods user-store (:_id user-data))
                                   (map payment/adapt-mongo-to-spec))]

             (util/>200 ctx (-> response-defaults
                                (merge
                                 {:user (merge
                                         {:verifiedEmail false}
                                         (select-keys user-data [:firstName
                                                                 :lastName
                                                                 :emailAddress
                                                                 :createdDate
                                                                 :birthDay
                                                                 :birthMonth
                                                                 :verifiedEmail
                                                                 :receiveStarbucksEmailCommunications])
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
                                 (select-keys user-data [:addresses :socialProfile]))
                                (dissoc :target-environment)))))

(defn profile [mimi user-store app-config]
  (-> {:methods
       {:get {:parameters {:query {:access_token String
                                   (s/optional-key :select) String
                                   (s/optional-key :ignore) String}}
              :response (fn [ctx]
                          (let [try-id ::profile
                                try-type :api]

                            (dcatch ctx
                                    (do
                                      (let [user-id (util/authenticated-user-id ctx)]
                                        (load-profile ctx mimi user-store user-id))))))}
        :put {:parameters {:query {:access_token String}
                           :body UpdateMongoUser}
              :response (fn [ctx]
                          (dcatch ctx
                             (update-user ctx (util/authenticated-user-id ctx) user-store mimi)))}}}
      (merge (util/common-resource :profile))))

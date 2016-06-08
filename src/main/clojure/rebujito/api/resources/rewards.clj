(ns rebujito.api.resources.rewards
  (:require
   [rebujito.protocols :as p]
   [rebujito.scopes :as scopes]
   [rebujito.api.time :as t]
   [rebujito.api.util :refer :all]
   [rebujito.api.resources :refer (domain-exception)]
   [cheshire.core :as json]
   [manifold.deferred :as d]
   [schema.core :as s]
   [yada.resource :refer [resource]]))

(def reward-every-x-points 100)
(def gold-threshold 300)
(def rewards-program {:programName "My Starbucks Rewards"
                      :numberOfTiers 2
                      :countryCodes ["ZA"]
                      :tierInfos [{:tierNumber 1
                                  :tierLevelName "Green"
                                  :tierPointsEntryThreshold 0
                                  :tierPointsExitThreshold gold-threshold
                                  :tierPointsReevaluationThreshold 0
                                  :tierPointsFreeItemThreshold reward-every-x-points}
                                  {:tierNumber 2
                                  :tierLevelName "Gold"
                                  :tierPointsEntryThreshold gold-threshold
                                  :tierPointsExitThreshold nil
                                  :tierPointsReevaluationThreshold gold-threshold
                                  :tierPointsFreeItemThreshold reward-every-x-points}]})

(defn points-needed-for-next-reward [points-now]
  (- reward-every-x-points (mod points-now reward-every-x-points)))

(defn points-earned-toward-next-free-reward [points-now]
  (- reward-every-x-points (points-needed-for-next-reward points-now)))

(defn points-needed-for-reevaluation [points-now]
  (if (>= points-now gold-threshold)
    (max 0 (- (* 2 gold-threshold) points-now))
    0))

(defn translate-mimi-rewards [rewards-response]
  (let [tier-name (-> rewards-response :tier :name)
        tier-date (-> rewards-response :tier :date)
        points-balance (-> rewards-response :tier :balance)]
    {:currentLevel tier-name
     :dateRetrieved (.toString (java.time.Instant/now))
     :pointsTotal points-balance
     :pointsNeededForNextLevel (-> rewards-response :tier :pointsUntilNextTier)
     :nextLevel (if (= tier-name "Green") "Gold" nil)
     :pointsNeededForNextFreeReward (points-needed-for-next-reward points-balance)
     :reevaluationDate (t/one-year-from tier-date)
     :pointsNeededForReevaluation (points-needed-for-reevaluation points-balance)
     :pointsEarnedTowardNextFreeReward (points-earned-toward-next-free-reward points-balance)

     ;; TODO the unknowns
     :cardHolderSinceDate nil
  }))

(defn rewards-response [mimi]
  (d/chain (p/rewards mimi {})
   translate-mimi-rewards
   #(merge
     rebujito.store.mocks/me-rewards
     {:rewardsProgram rewards-program}
     {:coupons []
      :myTiers []}
     %)))

(defn me-rewards [store mimi user-store authorizer authenticator]
 (-> {:methods
      {:get {:parameters {:query {:access_token String
                                  (s/optional-key :select) String
                                  (s/optional-key :ignore) String}}
             :response (fn [ctx]
                         (-> (d/let-flow [rewards (rewards-response mimi)]
                                         (>200 ctx rewards))
                             (d/catch clojure.lang.ExceptionInfo
                                 (fn [exception-info]
                                   (domain-exception ctx (ex-data exception-info))))))}}}

     (merge (common-resource :profile))
     (merge (access-control* authenticator authorizer {:get :rebujito.scopes/user}))))

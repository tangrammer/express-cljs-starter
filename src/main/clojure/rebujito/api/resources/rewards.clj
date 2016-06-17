(ns rebujito.api.resources.rewards
  (:require
   [rebujito.protocols :as p]
   [rebujito.scopes :as scopes]
   [rebujito.api.time :as t]
   [rebujito.api.util :refer :all]
   [rebujito.api.resources :refer (domain-exception)]
   [cheshire.core :as json]
   [rebujito.store.mocks :as m]
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
  (let [tier-name (-> rewards-response :tier :name (or "Green"))
        tier-date (-> rewards-response :tier :date (or (t/today)))
        points-balance (-> rewards-response :tier :balance (or 0))]
    {:currentLevel tier-name
     :dateRetrieved (.toString (java.time.Instant/now))
     :pointsTotal points-balance
     :pointsNeededForNextLevel (-> rewards-response :tier :pointsUntilNextTier (or 300))
     :nextLevel (if (= tier-name "Green") "Gold" nil)
     :pointsNeededForNextFreeReward (points-needed-for-next-reward points-balance)
     :reevaluationDate (str (t/one-year-from tier-date) "T23:59:59.999Z")
     :pointsNeededForReevaluation (points-needed-for-reevaluation points-balance)
     :pointsEarnedTowardNextFreeReward (points-earned-toward-next-free-reward points-balance)

     ;; TODO the unknowns
     :cardHolderSinceDate nil
  }))

(defn rewards-response [mimi card-number]

  (d/chain (p/rewards mimi card-number)
   translate-mimi-rewards
   #(merge
     m/me-rewards
     {:rewardsProgram rewards-program}
     {:coupons []
      :myTiers []}
     %)))

(defn me-rewards [store mimi user-store]
 (-> {:methods
      {:get {:parameters {:query {:access_token String
                                  (s/optional-key :locale) String
                                  (s/optional-key :select) String
                                  (s/optional-key :ignore) String}}
             :response (fn [ctx]
                         (-> (d/let-flow [user-id (:_id (authenticated-user ctx))
                                          user-data (p/find user-store user-id)
                                          card-number (-> user-data :cards first :cardNumber)
                                          rewards (rewards-response mimi card-number)]
                                         (>200 ctx rewards #_rebujito.store.mocks/me-rewards))
                             (d/catch clojure.lang.ExceptionInfo
                                 (fn [exception-info]
                                   (domain-exception ctx (ex-data exception-info))))))}}}

     (merge (common-resource :profile))))

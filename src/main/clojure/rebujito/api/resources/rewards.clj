(ns rebujito.api.resources.rewards
  (:require
   [rebujito.protocols :as p]
   [rebujito.scopes :as scopes]
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

(defn translate-mimi-rewards [rewards-response]
  (let [tier-name (-> rewards-response :tier :name)
        points-total (-> rewards-response :tier :balance)]
    {:currentLevel tier-name
     :dateRetrieved (.toString (java.time.Instant/now))
     :pointsTotal points-total
     :pointsNeededForNextLevel (-> rewards-response :tier :pointsUntilNextTier)
     :nextLevel (if (= tier-name "Green") "Gold" nil)
     :pointsNeededForNextFreeReward (points-needed-for-next-reward points-total)
  ;  :reevaluationDate nil
  ;  :pointsNeededForReevaluation 0
  ;  :cardHolderSinceDate nil
  ;  :pointsEarnedTowardNextFreeReward 0
  }))

(defn rewards-response [mimi]
  (d/chain (p/rewards mimi {})
   translate-mimi-rewards
   #(merge
     rebujito.store.mocks/me-rewards
     {:rewardsProgram rewards-program}
     {:coupons []}
     %)))

(defn me-rewards [store mimi user-store authorizer authenticator]
 (resource
  (-> {:methods
       {:get {:parameters {:query {:access_token String
                                   (s/optional-key :select) String
                                   (s/optional-key :ignore) String}}
              :consumes [{:media-type #{"application/json"}
                          :charset "UTF-8"}]
              :response (fn [ctx]
                          (-> (rewards-response mimi)
                              (d/chain
                               (fn [res]
                                 (>200 ctx res)))
                              (d/catch clojure.lang.ExceptionInfo
                                  (fn [exception-info]
                                    (domain-exception ctx (ex-data exception-info))))
                              (d/catch Exception
                                  #(>500* ctx (str "ERROR CAUGHT!" (.getMessage %))))))}}}

      (merge (common-resource :profile))
      (merge (access-control* authenticator authorizer {:get :rebujito.scopes/user})))))

(ns rebujito.api.resources.rewards
  (:require
   [taoensso.timbre :as log]
   [rebujito.protocols :as p]
   [rebujito.scopes :as scopes]
   [rebujito.api.time :as t]
   [rebujito.api.util :refer :all]
   [rebujito.util :refer (dtry error*)]
   [rebujito.api.resources :refer (domain-exception)]
   [cheshire.core :as json]
   [rebujito.store.mocks :as m]
   [manifold.deferred :as d]
   [schema.core :as s]
   [yada.resource :refer [resource]])
  (:import [java.time Instant]))

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

(defn mimi-to-rebujito-coupons-tr [mimi-coupon]
 {:couponCode "EFD" ;; earned free drink, or "BFB" birthday, "T3W" tier-3 welcome "WB3" welcome back to tier-3
  :name (:name mimi-coupon)
  :posCouponCode (:number mimi-coupon)
  :allowedRedemptionCount 1
  :voucherType "MSRPromotionalCoupon"
  :status (if (:redeemed mimi-coupon) "Redeemed" "Available")
  :redemptionCount 0
  :deliveryMethod "Email"
  :source "Unknown"

  :lastRedemptionDate "1904-01-01T00:00:00.0000000Z"

  ;; TODO to sort out these silly defaults, we need to speak to Brandon @ Micros
  :startDate (or (:validFromDate mimi-coupon) (-> (Instant/now) .toString))
  :issueDate (-> (Instant/now) .toString)
  :expirationDate (or (:validUntilDate mimi-coupon) "2099-12-31T00:00:00.0000000Z")
  })

(defn translate-mimi-rewards [rewards-response]
  (let [tier-name (-> rewards-response :tier :name (or "Green"))
        tier-date (-> rewards-response :tier :date (or (t/today)))
        points-balance (-> rewards-response :tier :balance (or 0))
        coupons (-> rewards-response :coupons (or []))]

    {:currentLevel tier-name
     :dateRetrieved (-> (Instant/now) .toString)
     :pointsTotal points-balance
     :pointsNeededForNextLevel (-> rewards-response :tier :pointsUntilNextTier (or 300))
     :nextLevel (if (= tier-name "Green") "Gold" nil)
     :pointsNeededForNextFreeReward (points-needed-for-next-reward points-balance)
     :reevaluationDate (str (t/one-year-from tier-date) "T23:59:59.999Z")
     :pointsNeededForReevaluation (points-needed-for-reevaluation points-balance)
     :pointsEarnedTowardNextFreeReward (points-earned-toward-next-free-reward points-balance)

     ;; TODO we might need to only output redeemed coupons
    ;  :coupons (map mimi-to-rebujito-coupons-tr (filter #(not (:redeemed %)) coupons))
     :coupons (map mimi-to-rebujito-coupons-tr coupons)

     ;; TODO the unknowns
     :cardHolderSinceDate nil
  }))

(defn rewards-response [mimi-balances card-number]
  (log/info "rewards-response/card-number=>" card-number)
  (->> mimi-balances
      translate-mimi-rewards
      (merge
        m/me-rewards
        {:rewardsProgram rewards-program}
        {:myTiers []})))

(defn me-rewards [store mimi user-store]
 (-> {:methods
      {:get {:parameters {:query {:access_token String
                                  (s/optional-key :locale) String
                                  (s/optional-key :select) String
                                  (s/optional-key :ignore) String}}
             :response (fn [ctx]
                         (-> (d/let-flow [user-id (:user-id (authenticated-data ctx))
                                          user-data (p/find user-store user-id)
                                          card-number (-> user-data :cards first :cardNumber)
                                          ; can test with:
                                        ; card-number "9623570900001"

                                          balances (when card-number
                                                     (p/balances mimi card-number))
                                          rewards (rewards-response balances card-number) #_rebujito.store.mocks/me-rewards]
                                         (>200 ctx rewards #_rebujito.store.mocks/me-rewards))
                             (d/catch clojure.lang.ExceptionInfo
                                 (fn [exception-info]
                                   (domain-exception ctx (ex-data exception-info))))))}}}

     (merge (common-resource :profile))))

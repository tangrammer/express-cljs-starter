(ns rebujito.api.resources.rewards-test
  (:require [rebujito.api.resources.rewards :refer [translate-mimi-rewards]]
            [clojure.test :refer :all]))

(def mimi-response {:programs
                     [{:program "MSR - Customer Tier" :balance 362 }
                      {:program "MSR - Employee" :balance 4 }
                      {:program "Employee Credit Card" :balance 0 }]
                    :tier
                     { :name "Gold"
                       :date "2016-03-18"
                       :balance 362
                       :pointsUntilNextTier -262 }})

(deftest translate-mimi-rewards-test
  (testing "translation"
    (let [translated (translate-mimi-rewards mimi-response)]
      (is (= (translated :currentLevel) "Gold"))
      (is (string? (translated :dateRetrieved)))
      (is (= (translated :pointsTotal) (-> mimi-response :tier :balance)))
      (is (= (translated :pointsTotal) 362))
      (is (= (translated :pointsNeededForNextLevel) (-> mimi-response :tier :pointsUntilNextTier)))
  )))

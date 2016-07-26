(ns rebujito.api.resources.rewards-test
  (:require [rebujito.api.resources.rewards :refer :all]
            [rebujito.base-test :refer (system-fixture *system*)]
            [clojure.test :refer :all]))

(def mimi-response {:programs
                     [{:code "MSR001" :program "SR - Customer" :balance 362}
                      {:code "MSR002" :program "SR - Customer Reward" :balance 12}
                      {:code "SGC001" :program "Starbucks Card" :balance 0 }]
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
      (is (= (translated :pointsNeededForNextLevel) (-> mimi-response :tier :pointsUntilNextTier))))))

(deftest points-needed-test
  (testing "that shit"
    (is (= 250
      (points-needed-for-next-reward 0)
      (points-needed-for-next-reward 250)))
    (is (= 1
      (points-needed-for-next-reward 249)
      (points-needed-for-next-reward 499)
      (points-needed-for-next-reward 749)))
    (is (= 50
      (points-needed-for-next-reward 200)
      (points-needed-for-next-reward 450)))
    (is (= 49
      (points-needed-for-next-reward 201)
      (points-needed-for-next-reward 451)))))

(deftest points-needed-for-reevaluation-test
  (testing "re-evaluation rules: how many more points do you need to stay on this tier"
    (is (= 0 (points-needed-for-reevaluation 0)))
    (is (= 0 (points-needed-for-reevaluation 749)))
    (is (= 750 (points-needed-for-reevaluation 750)))
    (is (= 749 (points-needed-for-reevaluation 751)))
    (is (= 1 (points-needed-for-reevaluation 1499)))
    (is (= 0 (points-needed-for-reevaluation 1500)))
    (is (= 0 (points-needed-for-reevaluation 1501)))))

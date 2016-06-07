(ns rebujito.api.resources.rewards-test
  (:require [rebujito.api.resources.rewards :refer :all]
            [rebujito.base-test :refer (system-fixture *system*)]
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
      (is (= (translated :pointsNeededForNextLevel) (-> mimi-response :tier :pointsUntilNextTier))))))

(deftest points-needed-test
  (testing "that shit"
    (is (= 100
      (points-needed-for-next-reward 0)
      (points-needed-for-next-reward 100)))
    (is (= 1
      (points-needed-for-next-reward 99)
      (points-needed-for-next-reward 199)
      (points-needed-for-next-reward 499)))
    (is (= 50
      (points-needed-for-next-reward 50)
      (points-needed-for-next-reward 150)))
    (is (= 49
      (points-needed-for-next-reward 51)
      (points-needed-for-next-reward 151)))))

(deftest points-needed-for-reevaluation-test
  (testing "re-evaluation rules: how many more points do you need to stay on this tier"
    (is (= 0 (points-needed-for-reevaluation 0)))
    (is (= 0 (points-needed-for-reevaluation 299)))
    (is (= 300 (points-needed-for-reevaluation 300)))
    (is (= 299 (points-needed-for-reevaluation 301)))
    (is (= 1 (points-needed-for-reevaluation 599)))
    (is (= 0 (points-needed-for-reevaluation 600)))
    (is (= 0 (points-needed-for-reevaluation 601)))))

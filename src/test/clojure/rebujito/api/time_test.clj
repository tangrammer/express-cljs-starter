(ns rebujito.api.time-test
  (:require [clojure.test :refer :all]
            [rebujito.api.time :as t]))

(deftest one-year-from-test
 (testing "date shit"
   (is (= "2017-03-18" (t/one-year-from "2016-03-18")))
   (is (= "2017-02-28" (t/one-year-from "2016-02-29")))))

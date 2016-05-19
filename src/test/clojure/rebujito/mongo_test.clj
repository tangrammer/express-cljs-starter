(ns rebujito.mongo-test
  (:require [clojure.test :refer :all]
            [rebujito.mongo :refer (generate-account-id id>mimi-id)]))

(deftest ids
  (doseq [seed ["42472395" "42485871"]]
    (let [t (generate-account-id seed)]
      (is (= seed (id>mimi-id (str t)))))))

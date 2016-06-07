(ns rebujito.api.resources.account-test
  (:require
   [rebujito.api-test :refer (print-body)]
   [rebujito.base-test :refer (system-fixture *system* *user-access-token* get-path  access-token-application access-token-user new-account-sb create-account new-sig  api-config)]
   [clojure.test :refer :all]
   ))

(use-fixtures :each (system-fixture #{:+mock-mimi :+ephemeral-db}))

(deftest test-create-account
  (testing "create-account-only"
    (create-account  (assoc (new-account-sb)
                            :birthDay "1"
                            :birthMonth "1"))))

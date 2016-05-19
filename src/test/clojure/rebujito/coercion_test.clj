(ns rebujito.coercion-test
  (:require [clojure.test :refer :all]
            [rebujito.mongo.schemas :refer (query-by-example-coercer)]
            [rebujito.store.mocks :refer (account)]
            [schema.coerce :as sc]
            [rebujito.mimi :as mim]
            [rebujito.api.resources.account :refer (CreateAccountMimiMapping)]
            [rebujito.coercion :refer :all]))



(deftest query-by-example-coercer-test
  (is
      (thrown? RuntimeException
               (query-by-example-coercer {:id "573c55dcc6c99c07fe49ff5" :wow true})))

  (is (some? (query-by-example-coercer {:id "573c55dcc6c99c07fe49ff5a" :wow true}))))




(deftest account-coercion
  (is (some? ((sc/coercer mim/CreateAccountSchema
                    CreateAccountMimiMapping) account))))

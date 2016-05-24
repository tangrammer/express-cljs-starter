(ns rebujito.coercion-test
  (:require [clojure.test :refer :all]
            [rebujito.mongo.schemas :refer (query-by-example-coercer)]
            [rebujito.store.mocks :refer (account)]
            [schema.core :as s]
            [cheshire.core :as json]
            [schema.coerce :as sc]
            [rebujito.mimi :as mim]
            [rebujito.api.resources.account :refer (CreateAccountMimiMapping create-account-coercer)]
            [rebujito.coercion :refer :all])
  (:import [schema.utils ErrorContainer]))

(defn not-schema-error [r]
  (not= (type r) ErrorContainer)
  )

(deftest query-by-example-coercer-test
  (is
   (thrown? RuntimeException
            (query-by-example-coercer {:id "573c55dcc6c99c07fe49ff5" :wow true})))

  (is (some? (query-by-example-coercer {:id "573c55dcc6c99c07fe49ff5a" :wow true}))))

(deftest account-coercion
  (is (not-schema-error (s/validate mim/CreateAccountSchema {:email "string",
                                                  :password "string",
                                                  :lastname "string",
                                                  :city "string",
                                                  :region "String",
                                                  :firstname "asd",
                                                  :postalcode "string",
                                                  :gender "male",
                                                  :mobile "String",
                                                  :birth {:dayOfMonth "string", :month "string"}})))


  (is (not-schema-error (s/validate mim/CreateAccountSchema {:firstname "Juan A."
                                      :lastname "Ruz"
                                      :password "xxxxxx"
                                      :email "juanantonioruz@gmail.com"
                                      :mobile "0034630051897"

                                      :city "Sevilla"
                                      :region "Andalucia"

                                      :postalcode "41003"
                                      :gender "male" ; (male|female)
                                      :birth {:dayOfMonth  "01"
                                              :month       "01"}
;                                      :birthday "1976-06-13" ; 'YYYY-MM-DD'
                                      })))

  (is (not-schema-error (s/validate mim/CreateAccountSchema {:firstname "Juan A."
                                                  :lastname "Ruz"
                                                  :password "xxxxxx"
                                                  :email "juanantonioruz@gmail.com"
                                                  :mobile "0034630051897"
                                                  :city "Sevilla"
                                                  :region "Andalucia"
                                                  :postalcode "41003"
                                                  :gender "male" ; (male|female)
                                                  :birth {:dayOfMonth  "01"
                                                          :month       "01"}})))

  (is (not-schema-error (create-account-coercer (assoc account :gender "male")))))

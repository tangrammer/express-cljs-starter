;; utils about schema coercion
(ns swarm.coercion
  (:require [schema.macros :refer (error!)]
            [schema.utils :refer (validation-error-explain)])
  (:import [schema.utils ErrorContainer]))


(defn throw-coercer-exceptions
  "wraps a coercer so it throws an error in error case.
By default p/schema catch all errors "
  [c]
  (fn [schema]
    (let [coercion-result (c schema)]
      (if (= ErrorContainer (type coercion-result))
        (->
         (str "COERCER_ERROR: " (validation-error-explain (->  coercion-result :error)))
         error!)
        coercion-result))))

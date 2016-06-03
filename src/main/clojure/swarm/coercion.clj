;; utils about schema coercion
(ns swarm.coercion
  (:import [schema.utils ErrorContainer]))


(defn throw-coercer-exceptions
  "wraps a coercer so it throws an error in error case.
By default p/schema catch all errors "
  [c]
  (fn [schema]
    (let [coercion-result (c schema)]
      (if (= schema.utils.ErrorContainer (type coercion-result))
        (->
         (str "COERCER_ERROR: " (schema.utils/validation-error-explain (->  coercion-result :error)))
         schema.macros/error!)
        coercion-result))))

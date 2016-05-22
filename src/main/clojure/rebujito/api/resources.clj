(ns rebujito.api.resources)

(defmulti domain-exception "dispatch on data meaning"
  (fn [ctx ex-data] (:type ex-data)))

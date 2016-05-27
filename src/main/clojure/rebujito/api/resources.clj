(ns rebujito.api.resources
  (:require   [manifold.deferred :as d]
              [rebujito.api.util :refer (>500* >400*)]))

(defmulti domain-exception "dispatch on data meaning"
  (fn [ctx ex-data] (:type ex-data)))

(defmethod domain-exception :api [ctx {:keys [status body]}]
  (condp = status
    400 (>400* ctx body)
    500 (>500* ctx body)))

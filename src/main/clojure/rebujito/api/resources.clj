(ns rebujito.api.resources
  (:require
   [taoensso.timbre :as log]
   [manifold.deferred :as d]
   [rebujito.api.util :refer (>500* >400* >base)]))

(defmulti domain-exception "dispatch on data meaning"
  (fn [ctx ex-data]
    (:type ex-data)))

(defmethod domain-exception :api [ctx {:keys [status body]}]
  (log/error "domain-exception::: api :::" status body)
  (condp = status
    400 (>400* ctx body)
    401 (>400* ctx body)
    500 (>500* ctx body)
    :else (d/error-deferred (ex-info body {:status status}))
    ))


(defmethod domain-exception :store [ctx {:keys [status body]}]
  (log/error "domain-exception::: store :::" status body)
  (condp = status
    400 (>400* ctx body)
    401 (>400* ctx body)
    500 (>500* ctx body)
    :else (d/error-deferred (ex-info body {:status status}))
    ))

(ns rebujito.api.resources
  (:require
   [taoensso.timbre :as log]
   [manifold.deferred :as d]
   [rebujito.api.util :refer (new>500* >500* >400* >base)]))

(defmulti domain-exception "dispatch on data meaning"
  (fn [ctx ex-data]
    ;; (log/info (type ex-data))
    ;; (log/info ex-data )
    ;; (log/error (:type ex-data))
    (:type ex-data)))


(defn- all-domain-exception [ctx {:keys [status code message] :as ex-data}]
  (condp = status
    400 (>400* ctx {:code code :message message})
    401 (>400* ctx {:code code :message message})
    500 (new>500* ctx {:status 500 :code code :message message})
    :else (do
            (log/error ":else " ex-data)
            (d/error-deferred (ex-info message ex-data)))))



(defmethod domain-exception :api [ctx ex-data]
  (log/error "domain-exception::: api :::" ex-data)
  (all-domain-exception ctx ex-data))

(defmethod domain-exception :store [ctx ex-data]
  (log/error "domain-exception::: store :::" ex-data)
  (all-domain-exception ctx ex-data))

(defmethod domain-exception :payment-gateway [ctx ex-data]
  (log/error "domain-exception::: payment-gateway :::" ex-data)
  (all-domain-exception ctx ex-data))


(defmethod domain-exception :mimi [ctx ex-data]
  (log/error "domain-exception::: mimi :::" ex-data)
  (all-domain-exception ctx ex-data))

(defmethod domain-exception :default [ctx ex-data]
  (log/error "domain-exception::: default :::" ex-data)
  (all-domain-exception ctx ex-data))

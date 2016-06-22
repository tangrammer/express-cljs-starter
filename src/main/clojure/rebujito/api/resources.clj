(ns rebujito.api.resources
  (:require
   [taoensso.timbre :as log]
   [manifold.deferred :as d]
   [rebujito.api.util :refer (new>500* >500* >400* >base)]))

(defmulti domain-exception "dispatch on data meaning"
  (fn [ctx ex-data]
    (log/info (type ex-data))
    (log/info ex-data)
    (:type ex-data)))


(defn- all-domain-exception [ctx {:keys [status body code message] :as ex-data}]
  (condp = status
    400 (>400* ctx {:code code :message message :body body})
    401 (>400* ctx {:code code :message message :body body})
    500 (new>500* ctx {:status 500 :code code :message message :body body})
    :else (d/error-deferred (ex-info (str "" body) {:status 500 :code code :message message :body body}))
    )
  )

(defmethod domain-exception :api [ctx {:keys [status body code message] :as ex-data}]
  (log/error "domain-exception::: api :::" status code message body)
  (all-domain-exception ctx ex-data)
  )

(defmethod domain-exception :store [ctx {:keys [status body code message] :as ex-data}]
  (log/error "domain-exception::: store :::" status code message body)
  (all-domain-exception ctx ex-data)
  )

(defmethod domain-exception :payment-gateway [ctx {:keys [status body code message] :as ex-data}]
  (log/error "domain-exception::: payment-gateway :::" status code message body)
  (all-domain-exception ctx ex-data)
  )

(defmethod domain-exception :default [ctx {:keys [status body code message] :as ex-data}]
  (log/error "domain-exception::: default :::" status code message body)
  (all-domain-exception ctx ex-data)
  )

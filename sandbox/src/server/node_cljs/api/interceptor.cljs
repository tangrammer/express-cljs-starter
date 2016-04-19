(ns node-cljs.api.interceptor
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [bidi.bidi :as bidi]
   [cljs.core.async :as async :refer [put! chan <! >! close!]]
   [node-cljs.log :as log]
   [node-cljs.api.error :as e]
   [cuerdas.core :as cuerdas]
   [schema.core :as s :include-macros true]))

(defn extract-method [resource-map method]
  (go
    (if (-> resource-map :methods method)
      (-> resource-map :methods method)
      (e/new-error  405  (cuerdas/format "Method %s Not Allowed" (cuerdas/upper (name method)))))))

(defn extract-resource [routes path]
  (go
    (let [r (:handler (bidi/match-route routes path))]
      (if-let [r (and (some? r) (r))]
        r
        (e/new-error  404 (cuerdas/format "Resource %s Not Found" (cuerdas/upper path)))))))

(defn validate-schemas [ctx method]
  (go
    (when-let [query-schema (-> method :parameters :query)]
      (try
        (s/validate  query-schema (-> ctx :req :query))
        (catch js/Error e
          (e/new-error  400 (cuerdas/format "Schema Validation Error: %s " (.-message e))))))))

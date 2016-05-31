(ns rebujito.api.util
  (:require [manifold.deferred :as d]
            [yada.interceptors :as i]
            [yada.security :as sec]
            [byte-streams :as bs]
            [yada.handler :as yh]
            [taoensso.timbre :as log]))

(defn >base [ctx status body]
  (log/info "base response >>>> " status body)
  (-> ctx :response (assoc :status status)
      (assoc :body body)))

(defn >400 [ctx body]
  (>base ctx 400 body))

(defn >400* [ctx body]
  (log/error "ERROR >>>> " body)
  (d/error-deferred (ex-info body {:status 400})))

(defn >500* [ctx body]
  (log/error "ERROR>>>>>" body)
  (d/error-deferred (ex-info body {:status 500})))

(defn >404 [ctx body]
  (>base ctx 404 body))

(defn >403 [ctx body]
  (>base ctx 403 body))

(defn >500 [ctx body]
  (>base ctx 500 body))

(defn >201 [ctx body]
  (>base ctx 201 body))

(defn >200 [ctx body]
  (>base ctx 200 body))

;[b (when (and (= manifold.stream.BufferedStream (type (-> ctx :request :body))) (nil? (-> ctx :parameters :body))) (-> ctx :request :body bs/to-string))]

(defn log-handler-req  [ctx]

  (log/info "REQ <<  " (:method ctx) (-> ctx :request :uri) " :::: " (-> ctx :parameters)  )

  ctx
  )

(defn log-handler-res  [ctx]

  (log/info "RES >>  "  (:response ctx)  #_(-> ctx :response :body bs/to-string))

  ctx
  )


(def default-interceptor-chain
  [i/available?
   i/known-method?
   i/uri-too-long?
   i/TRACE
   i/method-allowed?
   i/parse-parameters

   sec/authenticate ; step 1
   i/get-properties ; step 2
   sec/authorize ; steps 3,4 and 5
   i/process-request-body
   log-handler-req
   i/check-modification-time
   i/select-representation
   ;; if-match and if-none-match computes the etag of the selected
   ;; representations, so needs to be run after select-representation
   ;; - TODO: Specify dependencies as metadata so we can validate any
   ;; given interceptor chain
   i/if-match
   i/if-none-match
   i/invoke-method
   i/get-new-properties
   i/compute-etag
   sec/access-control-headers
   #_sec/security-headers
   i/create-response
   i/logging
   i/return
   ])

(defn common-resource
  ([desc]
   (let [n (if (keyword? desc)
             (if (namespace desc)
               (clojure.string/join "/" [(namespace desc) (name desc)])
               (name desc))
             desc)]
    (common-resource n n)))
  ([desc swagger-tag]
   {:description desc
;:interceptor-chain (into [log-handler]  (conj yh/default-interceptor-chain log-handler-end) )
    :interceptor-chain default-interceptor-chain
    :logger log-handler-res
    :produces [{:media-type #{"application/json"}
                :charset "UTF-8"}]
    :swagger/tags (if (vector? swagger-tag)
                    swagger-tag [swagger-tag])}))
(def access-control
  {:access-control {}})

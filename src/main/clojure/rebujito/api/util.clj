(ns rebujito.api.util
  (:require [manifold.deferred :as d]
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

(defn log-handler  [ctx]
  (let [body nil #_(when (= manifold.stream.BufferedStream (type (-> ctx :request :body)))
               (-> ctx :request :body bs/to-string))]
    (log/info "<< : " (:method ctx) (-> ctx :request :uri) " :::: "(-> ctx :request :query-string) " :::: "  (if body body "body nil"))
    ctx))

(defn log-handler-end  [ctx]
  (log/info ">> : " (:response ctx))
  ctx)

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
    :interceptor-chain (into [log-handler]  (conj yh/default-interceptor-chain log-handler-end) )
    :produces [{:media-type #{"application/json"}
                :charset "UTF-8"}]
    :swagger/tags (if (vector? swagger-tag)
                    swagger-tag [swagger-tag])}))
(def access-control
  {:access-control {}})

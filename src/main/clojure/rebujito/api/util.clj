(ns rebujito.api.util
  (:require [manifold.deferred :as d]
;            [yada.interceptors :as i]
            [manifold.stream :as stream]
 ;           [yada.security :as sec]
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
  (if  (>= (-> ctx :response :status) 400)
    (let [body (bs/to-string (-> ctx :response :body ))]
      (log/info "CALL >>  "  (:response ctx) " :: " (-> ctx :request :uri) " :::: " (-> ctx :parameters) " :: " body)
      (assoc-in ctx [:response :body] (bs/convert body java.nio.ByteBuffer)))
    (log/info "CALL >>  "  (:response ctx) " :: " (-> ctx :request :uri) " :::: " (-> ctx :parameters))))


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
    :logger log-handler
    :produces [{:media-type #{"application/json"}
                :charset "UTF-8"}]
    :swagger/tags (if (vector? swagger-tag)
                    swagger-tag [swagger-tag])}))
(def access-control
  {:access-control {}})

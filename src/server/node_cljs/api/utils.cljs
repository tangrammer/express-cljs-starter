(ns ^:figwheel-always node-cljs.api.utils
    (:require-macros [cljs.core.async.macros :refer [go]])
    (:require
     [bidi.bidi :as bidi]
     [cljs.core.async :as async :refer [put! chan <! >! close!]]
     [node-cljs.express :refer (app)]
     [node-cljs.log :as log]
     [node-cljs.api.protocols :as p]
     [schema.core :as s :include-macros true]))



(def GET ::get)
(def POST ::post)

(def routes (atom []))

(defn extract-query-params[req]
  (js->clj (.-query req) :keywordize-keys true))

(defn extract-path-params[req]
  (js->clj (.-params req) :keywordize-keys true))

(defn path-info [req res]
  (log/debug "path-info controller")
  (.json res #js {"path" (.-path req)}))


(defn default-route [req res]
  (let [path (.-path req)
        method (keyword (.toLowerCase (.-method req)))
        r (:handler (bidi/match-route @routes path))]
    (log/info ">>>>>>>>" (str method) path )
    (if (some? r)
      (let [resource-map (r)]
        (if-let [method-found (-> resource-map :methods method)]
          (let [ctx {:req{:query (extract-query-params req)
                          :params (extract-path-params req)}
                     :res res}]
            (when-let [query-schema (-> method-found :parameters :query)]
              (try
                (s/validate  query-schema (-> ctx :req :query))
                (let [r ((-> method-found :response) ctx)]
                  (p/send r res))
                (catch js/Error e
                  (do (.status res 400)
                      (.send res (.-message e)))
                  )
                ))
            )
          (do (.status res 405)
              (.send res nil))))
      (do  (.status res 404)
           (.send res nil)))))


(defn defroute [a b fn-controller]
  (condp = a
    GET (.get app b (fn [req res]
                      (let [req* {:query (js->clj (.-query req) :keywordize-keys true)
                                  :params (js->clj (.-params req) :keywordize-keys true)}
                              r (fn-controller req* res)]
                          (.json res r))))))

(defn defroute*
  "async"
  [a b fn-controller]
  (condp = a
    GET (.get app b (fn [req res]
                      (go (let [req* {:query (extract-query-params req)
                                      :params (extract-path-params req)}
                                r (<! (fn-controller req* res))]
                            (.json res r)))))

    POST (.post app b (fn [req res]
                        (log/info (.accepts req ['application/json']))
                        (go (let [req* {:query (js->clj (.-query req) :keywordize-keys true)
                                        :params (js->clj (.-params req) :keywordize-keys true)}
                                  r (<! (fn-controller req* res))]
                              (.json res r)))))
    ))

(defn resource [m]
  (fn [] m))

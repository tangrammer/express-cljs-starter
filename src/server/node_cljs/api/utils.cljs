(ns ^:figwheel-always node-cljs.api.utils
    (:require-macros [cljs.core.async.macros :refer [go]])
    (:require
     [bidi.bidi :as bidi]
     [cljs.core.async :as async :refer [put! chan <! >! close!]]
     [node-cljs.express :refer (app)]
     [node-cljs.log :as log]
     [node-cljs.api.protocols :as p]
     [node-cljs.api.interceptor :as i]
     [node-cljs.api.error :as e]
     [cuerdas.core :as cuerdas]
     [schema.core :as s :include-macros true]
     [node-cljs.api.macros :as m :include-macros true]))


(def GET ::get)
(def POST ::post)

(def routes (atom []))


(defn path-info [req res]
  (log/debug "path-info controller")
  (.json res #js {"path" (.-path req)}))


(defn extract-query-params[req]
  (js->clj (.-query req) :keywordize-keys true))

(defn extract-path-params[req]
  (js->clj (.-params req) :keywordize-keys true))

(defn default-route [req res]
  (let [path (.-path req)
        method (keyword (.toLowerCase (.-method req)))]
       (log/info ">>>>>>>>" (str method) path)
       (go
         (try
           (let [resource (m/<? (i/extract-resource @routes path))
                 method-found (m/<? (i/extract-method resource method))
                 ctx {:req{:query (extract-query-params req)
                           :params (extract-path-params req)
                           :cookies "TODO"}
                      :res res}]
             (m/<? (i/validate-schemas ctx method-found))
             (let [r ((-> method-found :response) ctx)]
               (p/send r res)))
           (catch js/Error e (e/send-error-response e res))))))


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

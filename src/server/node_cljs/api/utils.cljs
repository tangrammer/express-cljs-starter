(ns ^:figwheel-always node-cljs.api.utils
    (:require-macros [cljs.core.async.macros :refer [go]])
    (:require
     [cljs.core.async :as async :refer [put! chan <! >! close!]]
     [node-cljs.express :refer (app)]
     [node-cljs.log :as log]))

(declare GET POST)

(defn path-info [req res]
  (log/debug "path-info controller")
  (.json res #js {"path" (.-path req)}))

(defn default-route [req res]
  (log/debug "default-route controller")
  (.send res (str "default-route::" (.-path req))))


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
                      (go (let [req* {:query (js->clj (.-query req) :keywordize-keys true)
                                      :params (js->clj (.-params req) :keywordize-keys true)}
                               r (<! (fn-controller req* res))]
                           (.json res r)))))))

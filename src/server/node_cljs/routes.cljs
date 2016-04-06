(ns ^:figwheel-always node-cljs.routes
  (:require [node-cljs.express :refer [app]]
            [node-cljs.log :as log]))

(declare GET)

(defn path-info [req res]
  (log/debug "path-info controller")
  (.json res #js {"path" (.-path req)}))

(defn default-route [req res]
  (log/debug "default-route controller")
  (.send res (str "default-route::" (.-path req))))

(.use app
  (fn [req res next]
    (log/debug "start" (.-method req) (.-path req))
    (next)))

(.get app "/" #(.send %2 "home"))

(defn defroute [a b fn-controller]
  (condp = a
    GET (.get app b (fn [req res]
                      (let [req* {:params (js->clj (.-params req) :keywordize-keys true)}
                            r (fn-controller req* res)]
                        (.json res r))))))

(defroute GET "/v1/customer/:customerId"
  (fn [req res]
    #js {:customer (-> req :params :customerId)}))





(.get app "/1" path-info)
(.get app "/2" path-info)
(.get app "/3" path-info)


(.use app default-route)

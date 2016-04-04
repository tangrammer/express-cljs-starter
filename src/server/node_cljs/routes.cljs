(ns ^:figwheel-always node-cljs.routes
  (:require [node-cljs.express :refer [app]]
            [node-cljs.log :as log]))

(defn path-info [req res]
  (log/debug "path-info controller")
  (.json res #js {"path" (.-path req)}))

(defn default-route [req res]
  (log/debug "default-route controller")
  (.send res (.-path req)))

(.use app
  (fn [req res next]
    (log/debug "start" (.-method req) (.-path req))
    (next)))

(.get app "/" #(.send %2 "home"))

(.get app "/1" path-info)
(.get app "/2" path-info)
(.get app "/3" path-info)

(.use app default-route)
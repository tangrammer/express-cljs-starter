(ns rebujito.handler
  (:require [bidi.bidi :as bidi]
            [com.stuartsierra.component :as component]
            [rebujito.config :refer [config]]
            [yada.security :refer [verify]]
            [yada.yada :as yada]))

(defmethod verify :cookie
  [ctx {:keys [verify]}]
  (let [the-cookie (get-in ctx [:cookies (:cookie-name (config))])]
    (verify the-cookie)))

(defrecord Handler [api]
  component/Lifecycle
  (start [this]
    this)
  (stop [this] this)

  bidi/RouteProvider
  (routes [_]
    ["/api/v1" (yada/swaggered
                (:routes api)
                {:info     {:title       "Rebujito REST API"
                            :version     "1.0"
                            :description "Having good times with clojure and rest"}
                 :basePath "/api/v1"})]))

(defn handler []
  (map->Handler {}))

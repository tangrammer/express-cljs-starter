(ns rebujito.handler
  (:require [bidi.bidi :as bidi]
            [com.stuartsierra.component :as component]
            [rebujito.config :refer [config]]
            [yada.yada :as yada]))

(defrecord Handler [api]
  component/Lifecycle
  (start [this]
    this)
  (stop [this] this)

  bidi/RouteProvider
  (routes [_]
    ["/starbucks/v1" (yada/swaggered
                (:routes api)
                {:info     {:title       "Rebujito REST API"
                            :version     "1.0"
                            :description "Having good times with clojure and rest"}
                 :basePath "/starbucks/v1"})]))

(defn handler []
  (map->Handler {}))

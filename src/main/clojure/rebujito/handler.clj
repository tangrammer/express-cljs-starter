(ns rebujito.handler
  (:require [bidi.bidi :as bidi]
            [com.stuartsierra.component :as component]
            [yada.yada :as yada]))

(defrecord Handler [resources signer]
  component/Lifecycle
  (start [this]
    this)
  (stop [this] this)

  bidi/RouteProvider
  (routes [_]
    ["/api/v1" (yada/swaggered
               (:routes resources)
               {:info     {:title       "Rebujito REST API"
                           :version     "1.0"
                           :description "Having good times with clojure and rest"}
                :basePath "/api/v1"})]))

(defn handler []
  (map->Handler {}))

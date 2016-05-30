(ns rebujito.webserver.handler
  (:require [bidi.bidi :as bidi]
            [byte-streams :as bs]
            [com.stuartsierra.component :as component]
            [rebujito.config :refer [config]]
            [yada.yada :as yada]))

(defrecord Handler [api base-url]
  component/Lifecycle
  (start [this]
    this)
  (stop [this] this)

  bidi/RouteProvider
  (routes [_]
    ["" [[base-url (yada/swaggered
                            (:routes api)
                            {:info     {:title       "Rebujito REST API"
                                        :version     "1.0"
                                        :description "Having good times with clojure and rest"}
                             :basePath base-url})]
         [true (yada/handler (fn [ctx]
                               (println ">>>>>>NOT_FOUND")
                               (clojure.pprint/pprint ctx)
                               (when (= manifold.stream.BufferedStream (type (:body ctx)))
                                 (print "BODY: ")
                                 (clojure.pprint/pprint (-> ctx :body bs/to-string))
                                 )
                               (println ">>>" )
                               {:status 404 :body "Not found"}))]]
     ]))

(defn handler [config]
  (map->Handler config))

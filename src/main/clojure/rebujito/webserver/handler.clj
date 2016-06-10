(ns rebujito.webserver.handler
  (:require [bidi.bidi :as bidi]
            [byte-streams :as bs]
            [rebujito.api.util :as util]
            [rebujito.config :refer [config]]
            [rebujito.api.resources.content :as content]
            [com.stuartsierra.component :as component]
            [yada.yada :as yada]
            [yada.resource :refer (resource)]))

(defrecord Handler [api base-url]
  component/Lifecycle
  (start [this]
    this)
  (stop [this] this)

  bidi/RouteProvider
  (routes [_]
    ["" [[base-url (yada/swaggered
                            (:routes api)
                            {:info     {:title       "Starbucks API"
                                        :version     "1.0"
                                        :description "SWARM Implementation"}
                             :basePath base-url})]

         [(str base-url "/content/sitecore/content") [[true (yada/handler content/terms-json)]]]

         [true
          (resource
           (-> {:methods
                {:* {
                     :response (fn [ctx]
                                 (println ">>>>>>NOT_FOUND")
                                 (clojure.pprint/pprint ctx)
                                 (when (= manifold.stream.BufferedStream (type (:body ctx)))
                                   (print "BODY: ")
                                   (clojure.pprint/pprint (-> ctx :body bs/to-string)))

                                 (println ">>>")
                                 (util/>404 ctx "Not Found"))
                     :consumes [{:media-type #{"application/x-www-form-urlencoded" "application/json"}
                                 :charset "UTF-8"}]}}}

               (merge (util/common-resource :not-found-default))
               (merge util/access-control)
               (merge {:produces #{"application/json" "text/html" "text/plain"}})))]]]))

(defn handler [config]
  (map->Handler config))

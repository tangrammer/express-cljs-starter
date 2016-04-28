;;most of the utility fns in this ns are taken from juxt/modular
(ns rebujito.system
  "Components and their dependency reationships"
  (:refer-clojure :exclude (read))
  (:require
    [rebujito.api :as api]
    [rebujito.handler :as wh]
    [rebujito.security :as security]
    [rebujito.logging :as log-levels]
    [clojure.edn :as edn]
    [com.stuartsierra.component :refer [system-map system-using using] :as component]

    [modular.aleph :refer [new-webserver]]
    [modular.bidi :refer [new-router new-web-resources new-redirect]]
    [taoensso.timbre :as log]
    [rebujito.config :refer [config]]
    )
  (:import [java.util Date]))

(defn swagger-ui-components [system]
  (assoc system
    :swagger-ui
    (new-web-resources
      :key :swagger-ui
      :uri-context "/swagger-ui"
      :resource-prefix "META-INF/resources/webjars/swagger-ui/2.1.4")))



(defn webserver [config]
  (->
    (new-webserver :port (-> config :yada :port) :raw-stream? true)
    (using {})))

(defn new-system-map [config]
  (apply system-map
         (apply concat
                (->
                 {
                  :docsite-router (new-router)

                  :security (security/new-security config)

                  :api (api/new-api-component )

                  :yada (wh/handler)

                  :webserver (webserver config)

                  :jquery (new-web-resources
                           :key :jquery
                           :uri-context "/jquery"
                           :resource-prefix "META-INF/resources/webjars/jquery/2.1.3")}

                 (swagger-ui-components)))))

(defn new-dependency-map
  []
  {
   :webserver {:request-handler :docsite-router}
   :api [:security]
   :yada [:api]
   :docsite-router [:swagger-ui :yada :jquery]})

(defn new-production-system
  "Create the production system"
  ([opts]
   (-> (new-system-map (merge (config :prod) opts))
       (system-using (new-dependency-map))))
  ([] (new-production-system {})))

(log/set-config! log-levels/timbre-info-config)

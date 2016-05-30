(ns rebujito.system
  "Components and their dependency reationships"
  (:refer-clojure :exclude (read))
  (:require
   [rebujito.payment-gateway :as payment-gateway]
   [com.stuartsierra.component :refer [system-map system-using using] :as component]
   [modular.aleph :refer [new-webserver]]
   [modular.mongo :refer [new-mongo-database new-mongo-connection]]
   [modular.bidi :refer [new-router new-web-resources new-redirect]]
   [rebujito.api :as api]
   [rebujito.security.auth :as oauth ]
   [rebujito.security.encrypt :as encrypt]
   [rebujito.security.jwt :as jwt ]
   [rebujito.config :refer [config]]
   [rebujito.webserver.handler :as wh]
   [rebujito.logging :as log-levels]
   [rebujito.store :as store]
   [rebujito.mongo :refer (new-user-store)]
   [rebujito.mimi :as mimi]
   [taoensso.timbre :as log])
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
   (new-webserver :port (if (= String (type (-> config :yada :port)))
                          (read-string (-> config :yada :port))
                          (-> config :yada :port)
                          ) :raw-stream? true)
    (using {})))

(defn new-system-map [config]
  (apply system-map
         (apply concat
                (->
                 {
                  :docsite-router (new-router)

                  :db (new-mongo-database (-> config :mongo))

                  :db-conn (new-mongo-connection)

                  :store (store/new-prod-store)

                  :user-store (new-user-store (:auth config))

                  :mimi (mimi/new-prod-mimi (:mimi config))

                  :crypto (encrypt/new-sha256-encrypter (:auth config))

                  :authorizer (oauth/new-authorizer)

                  :authenticator (jwt/new-authenticator (:auth config))

                  :payment-gateway (payment-gateway/new-prod-payment-gateway (-> config  :payment-gateway :paygate))

                  :api (api/new-api-component)

                  :yada (wh/handler (:yada config))

                  :webserver  (webserver config)

                  :jquery (new-web-resources
                           :key :jquery
                           :uri-context "/jquery"
                           :resource-prefix "META-INF/resources/webjars/jquery/2.1.4")}

                 (swagger-ui-components)))))

(defn new-dependency-map
  []
  {
   :webserver {:request-handler :docsite-router}
   :db-conn {:database :db}
   :user-store [:db-conn]
   :authorizer [:authenticator]
   :store []
   :api [:store :mimi :user-store :authorizer :crypto :authenticator :payment-gateway]
   :yada [:api]
   :docsite-router [:swagger-ui :yada :jquery]})

(defn new-production-system
  "Create the production system"
  ([opts]
   (-> (new-system-map (merge (config :prod) opts))
       (system-using (new-dependency-map))))
  ([] (new-production-system {})))

(log/set-config! log-levels/timbre-info-config)

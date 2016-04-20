;;most of the utility fns in this ns are taken from juxt/modular
(ns co.za.swarmloyalty.rebujito.system
  "Components and their dependency reationships"
  (:refer-clojure :exclude (read))
  (:require
    [co.za.swarmloyalty.rebujito.api :as api]
    [co.za.swarmloyalty.rebujito.handler :as wh]
    [co.za.swarmloyalty.rebujito.auth :as auth]
    [co.za.swarmloyalty.rebujito.logging :as log-levels]
    [clojure.edn :as edn]
    [com.stuartsierra.component :refer [system-map system-using using] :as component]
    [environ.core :refer [env]]
    [modular.aleph :refer [new-webserver]]
    [modular.bidi :refer [new-router new-web-resources new-redirect]]
    [taoensso.timbre :as log]
    [yada.security :refer [verify]]
    )
  (:import [java.util Date]))

(defn- load-env-value
  ([y]
   (load-env-value y false))
  ([y convert?]
   (let [x (env y)]
     (if convert?
       (edn/read-string x)
       x))))

(defn config
  "Return a map of the static configuration used in the component
  constructors."
  []
  {:cookie-name (load-env-value :rebujito-cookie-name)
   :auth        {:secret-key "lp0fTc2JMtx8"}
   :yada        {:port (load-env-value :rebujito-yada-port true)}})

(defn swagger-ui-components [system]
  (assoc system
    :swagger-ui
    (new-web-resources
      :key :swagger-ui
      :uri-context "/swagger-ui"
      :resource-prefix "META-INF/resources/webjars/swagger-ui/2.1.4")))

(defmethod verify :cookie
  [ctx {:keys [verify]}]
  (let [the-cookie (get-in ctx [:cookies (load-env-value :rebujito-cookie-name)])]
    (verify the-cookie)))

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

                  :signer (auth/signer config)


                  :resources (api/new-api-component )

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
   :resources [:signer]
   :yada [:resources :signer]
   :docsite-router [:swagger-ui :yada :jquery]})

(defn new-production-system
  "Create the production system"
  ([opts]
   (-> (new-system-map (merge (config) opts))
       (system-using (new-dependency-map))))
  ([] (new-production-system {})))

(log/set-config! log-levels/timbre-info-config)

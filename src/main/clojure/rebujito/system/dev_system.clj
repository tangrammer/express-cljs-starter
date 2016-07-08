(ns rebujito.system.dev-system
  "Dev Components and their dependency reationships"
  (:require
   [rebujito.payment-gateway :refer (new-mock-payment-gateway)]
   [com.stuartsierra.component :as component :refer (using)]
   [rebujito.config :refer (config)]
   [rebujito.store :refer (new-mock-store)]
   [rebujito.mimi.mocks :refer (new-mock-mimi)]
   [taoensso.timbre :as log]
   [rebujito.mailer :refer (new-mock-mailer)]
   [rebujito.system :refer (new-system-map new-dependency-map)]))


(def mod-defs
  {:system-mods
   {
    :+mock-store
    (fn [config]
      (log/warn "using :+mock-store profile in dev-system")
      (fn [system-map]
        (-> system-map
            (assoc :store (new-mock-store)))))
    :+mock-mailer
    (fn [config]
      (log/warn "using :+mock-mailer profile in dev-system")
      (fn [system-map]
        (-> system-map
            (assoc :mailer (new-mock-mailer {})))))
    :+ephemeral-db
    (fn [config]
      (log/warn "using :+ephemeral-db to start mongo with no data inside")
      (fn [system-map]
        (-> system-map
            (assoc
             :user-store (rebujito.mongo/new-user-store (:auth config) true)
             :token-store (rebujito.mongo/new-token-store (:auth config) true)
             :webhook-store (rebujito.mongo/new-webhook-store (:auth config) true)
;             500 increment
 ;;            :counter-store (rebujito.mongo/new-counter-store (:auth config) true {:digital-card-number (read-string (format "96235709%05d" 0))})
             :api-client-store (rebujito.mongo/new-api-key-store (:auth config) true)))))
    :+mock-mimi
    (fn [config]
      (log/warn "using :+mock-mimi profile in dev-system")
      (fn [system-map]
        (-> system-map
            (assoc :mimi (new-mock-mimi (:mimi config))))))
    :+mock-payment-gateway
    (fn [config]
        (log/warn "using :+mock-payment-gateway profile in dev-system")
        (fn [system-map]
            (-> system-map
                (assoc :payment-gateway (new-mock-payment-gateway (:payment-gateway config))))))}
   :dependency-mods
   {:+mock-store (fn [config]
                   (fn [dependency-map]
;                     (println "using :mock-store  in dependency dev-system")
                     dependency-map))
    :+mock-mimi (fn [config]
                   (fn [dependency-map]
 ;                    (println "using :mock-mimi  in dependency dev-system")
                     dependency-map))
    :+mock-payment-gateway (fn [config]
                    (fn [dependency-map]
  ;                      (println "using :mock-payment-gateway  in dependency dev-system")
                      dependency-map))}})



(defn new-dev-system
  "Create a development system"
  ([] (new-dev-system #{:+mock-mimi :+mock-store :+ephemeral-db :+mock-mailer}))
  ([opts]  (new-dev-system opts (config :dev)))
  ([opts config]
   (component/system-using
    ((apply comp
            (map #(% config) (remove nil? (for [mod opts] (get-in mod-defs [:system-mods mod])))))
     (new-system-map config))
    ((apply comp
            (map #(% config) (remove nil? (for [mod opts] (get-in mod-defs [:dependency-mods mod])))))
     (new-dependency-map)))))

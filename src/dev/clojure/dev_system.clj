(ns dev-system
  "Dev Components and their dependency reationships"
  (:require 
    [com.stuartsierra.component :as component :refer (using)]
    [rebujito.config :refer (config)]
    [rebujito.store :refer (new-mock-store)]
    [rebujito.mimi :refer (new-mock-mimi)]
    [rebujito.payment-gateway :refer (new-mock-payment-gateway)]
    [rebujito.system :refer (new-system-map new-dependency-map)]))


(def mod-defs
  {:system-mods
   {
    :+mock-store
    (fn [config]
      (println "using :+mock-store profile in dev-system")
      (fn [system-map]
        (-> system-map
            (assoc :store (new-mock-store)))))
    :+mock-mimi
    (fn [config]
      (println "using :+mock-mimi profile in dev-system")
      (fn [system-map]
        (-> system-map
            (assoc :mimi (new-mock-mimi (:mimi config))))))
    :+mock-payment-gateway
    (fn [config]
        (println "using :+mock-payment-gateway profile in dev-system")
        (fn [system-map]
            (-> system-map
                (assoc :payment-gateway (new-mock-payment-gateway (:payment-gateway config))))))}
   :dependency-mods
   {:+mock-store (fn [config]
                   (fn [dependency-map]
                     (println "using :mock-store  in dependency dev-system")
                     dependency-map))
    :+mock-mimi (fn [config]
                   (fn [dependency-map]
                     (println "using :mock-mimi  in dependency dev-system")
                     dependency-map))
    :+mock-payment-gateway (fn [config]
                    (fn [dependency-map]
                        (println "using :mock-payment-gateway  in dependency dev-system")
                        dependency-map))}})



(defn new-dev-system
  "Create a development system"
  ([] (new-dev-system #{:+mock-store}))
  ([opts]
   (let [config (config :dev)]
     (component/system-using
      ((apply comp
              (map #(% config) (remove nil? (for [mod opts] (get-in mod-defs [:system-mods mod])))))
       (new-system-map config))
      ((apply comp
              (map #(% config) (remove nil? (for [mod opts] (get-in mod-defs [:dependency-mods mod])))))
       (new-dependency-map))))))

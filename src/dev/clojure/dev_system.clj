(ns dev-system
  "Dev Components and their dependency reationships"
  (:require
   [com.stuartsierra.component :as component :refer (using)]
   [rebujito.config :refer (config)]
   [rebujito.store :refer (new-mock-store)]
   [rebujito.system :refer (new-system-map new-dependency-map)]))


(def mod-defs
  {:system-mods
   {:+mock-store
    (fn [config]
      (println "using :+mock-store profile in dev-system")
      (fn [system-map]
        (-> system-map
            (assoc :store (new-mock-store)))))}
   :dependency-mods
   {:+mock-store (fn [config]
                   (fn [dependency-map]
                     (println "using :mock-store profile in dependency dev-system")
                     dependency-map))}}

  )

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
       (new-dependency-map))
      ))))

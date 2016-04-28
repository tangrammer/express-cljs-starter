(ns dev-system
  "Dev Components and their dependency reationships"
  (:require
   [com.stuartsierra.component :as component :refer (using)]
   [rebujito.config :refer (config)]
   [rebujito.system :refer (new-system-map new-dependency-map)]
   ))


(def mod-defs
  {:system-mods
   {:dev
    (fn [config]
      (println "connfgigg dev")
      (fn [system-map]
        (-> system-map
;            (assoc :schema (database-schema ))
)))}
   :dependency-mods
   {:dev (fn [config]
           (fn [dependency-map]
             (println "map-connfgigg dev")
             #_(merge dependency-map
                    {:webserver {:request-handler :docsite-router}})
                                        ;             #_(dissoc dependency-map :x :y :z)
             dependency-map
             ))}}

  )

(defn new-dev-system
  "Create a development system"

  ([] (new-dev-system #{:dev}))
  ([env]
   (let [config (config :dev)]
     (component/system-using
      ((apply comp
              (map #(% config) (remove nil? (for [mod env] (get-in mod-defs [:system-mods mod])))))
       (new-system-map config))
      ((apply comp
              (map #(% config) (remove nil? (for [mod env] (get-in mod-defs [:dependency-mods mod])))))
       (new-dependency-map))
      ))))

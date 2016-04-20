(ns dev-system
  "Dev Components and their dependency reationships"
  (:require
   [bidi.bidi :refer [RouteProvider]]
   [bidi.ring :refer (make-handler)]
   [rebujito.system :refer (config new-system-map new-dependency-map)]
   [clojure.java.io :as io]
   [com.stuartsierra.component :as component :refer (using)]
   [modular.aleph :refer (new-webserver)]
   [modular.bidi :refer (new-router new-web-resources new-redirect)])




  )


(def mod-defs
  {:system-mods
   {:dev
    (fn [config]
      (fn [system-map]
        (-> system-map
;            (assoc :schema (database-schema ))
)))}
   :dependency-mods
   {:dev (fn [config]
           (fn [dependency-map]
             (merge dependency-map
                    {:webserver {:request-handler :docsite-router}
                     })
;             #_(dissoc dependency-map :x :y :z)
             ))}}

  )

(defn new-dev-system
  "Create a development system"

  ([] (new-dev-system #{:dev}))
  ([env]
   (let [config (config)]
     (component/system-using
      ((apply comp
              (map #(% config) (remove nil? (for [mod env] (get-in mod-defs [:system-mods mod])))))
       (new-system-map config))
      ((apply comp
              (map #(% config) (remove nil? (for [mod env] (get-in mod-defs [:dependency-mods mod])))))
       (new-dependency-map))
      ))))

(ns rebujito.api
  (:require
    [com.stuartsierra.component :as component]
    [plumbing.core :refer [fnk defnk]]
    [schema.core :as s]
    [rebujito.resources :as resources]))

(defn api [store security]
  ["/me" [[[ "/" :id "/fake"] (-> (resources/fake store)
                                  (assoc :id ::fake))]
          ["/cards/register-digital"
           [["" (-> (resources/register-digital-card store)
                    (assoc :id ::index))]]]]])

(s/defrecord ApiComponent [store security]
  component/Lifecycle
  (start [component]
    (assoc component :routes (api store security)))
  (stop [component]
        component))

(defn new-api-component []
  (->
   (map->ApiComponent {})))

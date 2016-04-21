(ns rebujito.api
  (:require
    [com.stuartsierra.component :as component]
    [plumbing.core :refer [fnk defnk]]
    [schema.core :as s]
    [rebujito.resources :as resources]))

(defn api [security]
  ["/me" [[[ "/" :id "/fake"] (-> (resources/fake)
                                  (assoc :id ::fake))]
          ["/cards/register-digital"
           [["" (-> (resources/register-digital-card)
                    (assoc :id ::index))]]]]])

(s/defrecord ApiComponent [security]
  component/Lifecycle
  (start [component]
    (assoc component
           :routes (api security)))
  (stop [component]))

(defn new-api-component []
  (->
   (map->ApiComponent {})))

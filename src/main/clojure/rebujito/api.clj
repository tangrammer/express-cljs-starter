(ns rebujito.api
  (:require
    [com.stuartsierra.component :as component]
    [plumbing.core :refer [fnk defnk]]
    [schema.core :as s]
    [rebujito.resources :as resources]))

(defn api [signer]
  ["/me" [[[ "/" :id "/fake"] (-> (resources/fake)
                                  (assoc :id ::fake))]
          ["/cards/register-digital"
           [["" (-> (resources/register-digital-card)
                    (assoc :id ::index))]
            [["/" :entry] (-> (resources/new-entry-resource)
                              (assoc :id ::entry))]]]]])

(s/defrecord ApiComponent [signer]
  component/Lifecycle
  (start [component]
    (assoc component
           :routes (api signer)))
  (stop [component]))

(defn new-api-component []
  (->
   (map->ApiComponent {})))

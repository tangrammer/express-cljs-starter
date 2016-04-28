(ns rebujito.store
  (:require
   [rebujito.protocols :as protocols]
   [com.stuartsierra.component :refer [system-map system-using using] :as component]
   [plumbing.core :refer [defnk]]
   [rebujito.mocks :as mocks]

   ))


(defrecord ProdStore []
  component/Lifecycle
  (start [this]
    this)
  (stop [this] this)
  protocols/Store
  (get-card [this]
    (assoc mocks/card :id :prod)))

(defrecord MockStore []
  component/Lifecycle
  (start [this]
    this)
  (stop [this] this)
  protocols/Store
  (get-card [this]
    (assoc mocks/card :id :dev))
)


(defn new-prod-store []
  (map->ProdStore {}))

(defn new-mock-store []
  (map->MockStore {}))

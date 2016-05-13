(ns rebujito.mimi
  (:require
   [rebujito.protocols :as protocols]
   [org.httpkit.client :as http]
   [byte-streams :as bs]
   [com.stuartsierra.component  :as component]
   [rebujito.store.mocks :as mocks]))

(defrecord ProdMimi [base-url]
  component/Lifecycle
  (start [this]
    this)
  (stop [this] this)
  protocols/Mimi
  (create-account [this data]
    (let [{:keys [status body]} @(http/get base-url)]
      [status (-> body
                  vector
                  (conj :prod-mimi))])))


(defrecord MockMimi [base-url]
  component/Lifecycle
  (start [this]
    this)
  (stop [this] this)
  protocols/Mimi
  (create-account [this data]
    ["200" ["Created" "Resource Created" :mock-mimi]]))

(defn new-prod-mimi [mimi-config]
  (map->ProdMimi {:base-url (:base-url mimi-config)}))

(defn new-mock-mimi [mimi-config]
  (map->MockMimi {:base-url (:base-url mimi-config)}))

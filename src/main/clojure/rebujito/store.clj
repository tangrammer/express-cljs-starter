(ns rebujito.store
  (:require
   [rebujito.protocols :as protocols]
   [com.stuartsierra.component :refer [system-map system-using using] :as component]
   [plumbing.core :refer [defnk]]))


(defrecord ProdStore []
  component/Lifecycle
  (start [this]
    this)
  (stop [this] this)
  protocols/Auth
  (sign [this data expire-in]
    (let [token-id (.toString (UUID/randomUUID))]
     (-> (token/claims data token-id expire-in)
         (token/sign secret-key))))

  (unsign [this jwt]
    (token/unsign jwt secret-key)))

(defn new-prod-store []
  (map->ProdStore {}))

(defn new-mock-store []
  (map->MockStore {}))

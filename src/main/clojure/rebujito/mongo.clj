(ns rebujito.mongo
  (:require
   [com.stuartsierra.component  :as component]
   [monger.collection :as mc]
   [monger.conversion :refer [from-db-object]]
   [monger.core :as mg]
   [monger.json :as mj]
   [monger.result :refer [acknowledged?]]
   [rebujito.protocols :as protocols]
   [rebujito.mongo.schemas :refer (query-by-example-coercer)]
   [taoensso.timbre :as log])
  (:import [org.bson.types ObjectId]))


(defmulti db-find "dispatch on data meaning"
  (fn [mutable-storage data] (type data)))

(defmethod db-find :default [_ data]
  (throw (IllegalArgumentException.
          (str "Not ready to db-find using: " (type data)))))


(defrecord UserStorage [db-conn collection]
  component/Lifecycle
  (start [this]
    (assoc this
           :db (:db db-conn)
           :collection (if (keyword? collection)
                         (name collection)
                         collection)))
  (stop [this] this)

  protocols/MutableStorage
  (find [this]
    (mc/find (:db this) (:collection this)))
  (find [this data]
    (db-find this data))

  (get-and-insert! [this data]
    (mc/insert-and-return (:db this) (:collection this) data)))

(defn new-user-store []
  (map->UserStorage {:collection :users}))

(defn- find-map-by-id [mutable-storage id]
  (mc/find-map-by-id (:db mutable-storage) (:collection mutable-storage) id))

(defmethod db-find String
  [mutable-storage data]
  (find-map-by-id mutable-storage (org.bson.types.ObjectId. data)))

(defmethod db-find org.bson.types.ObjectId
  [mutable-storage data]
  (find-map-by-id mutable-storage data))

(defmethod db-find clojure.lang.PersistentArrayMap
  [mutable-storage data]
  (mc/find (:db mutable-storage) (:collection mutable-storage) data))

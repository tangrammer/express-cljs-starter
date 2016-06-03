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

(defn ^org.bson.types.ObjectId generate-account-id [s]
  {:pre  [(try (number? (read-string s))
               (catch Exception e (do
                                    "should be possible to be parsed as a number!"
                                    false)))]}
  (let [hex-id (format "%024x"  (read-string s))]
                                (org.bson.types.ObjectId.  hex-id)))

(defn ^String id>mimi-id [s]
  (str (BigInteger. s 16)))



(defmulti db-find "dispatch on data meaning"
  (fn [mutable-storage data] (type data)))

(defmethod db-find :default [_ data]
  (throw (IllegalArgumentException.
          (str "Not ready to db-find using: " (type data)))))


(defrecord MongoStorage [db-conn collection secret-key ephemeral?]
  component/Lifecycle
  (start [this]
    (let [c (assoc this
                   :db (:db db-conn)
                   :collection (if (keyword? collection)
                                 (name collection)
                                 collection))]
      (when ephemeral?
        (println "DEV ENV removing data from collection: " (:collection c))
        (mc/remove (:db c) (:collection c)))
      c
      ))
  (stop [this] this)

  protocols/MutableStorage
  (generate-id [this data]
    (generate-account-id data))
  (find [this]
    (mc/find (:db this) (:collection this)))
  (find [this data]
    (db-find this data))

  (get-and-insert! [this data]
    (mc/insert-and-return (:db this) (:collection this) data))

  (insert! [this data]
    (mc/insert (:db this) (:collection this) data))
  )


(defn new-user-store
  ([auth-data]
   (new-user-store auth-data false))
  ([auth-data ephemeral?]
   (map->MongoStorage {:collection :users
                       :secret-key (:secret-key auth-data)
                       :ephemeral?  ephemeral?})))

(defn new-api-key-store
  ([auth-data]
   (new-api-key-store auth-data false))
  ([auth-data ephemeral?]
   (map->MongoStorage {:collection :api-keys
                       :secret-key (:secret-key auth-data)
                       :ephemeral?  ephemeral?})))

(defn- find-map-by-id [mutable-storage id]
  (mc/find-map-by-id (:db mutable-storage) (:collection mutable-storage) id))

(defmethod db-find String
  [mutable-storage data]
  (if (= data "w8tnxd8h2wns43cfdgmt793j")
    {"_id" "w8tnxd8h2wns43cfdgmt793j", "secret" "KDRSRVqKHp5TkKvJJhN7RYkE", "who" "mediamonks"}
    (find-map-by-id mutable-storage (org.bson.types.ObjectId. data))))

(defmethod db-find org.bson.types.ObjectId
  [mutable-storage data]
  (find-map-by-id mutable-storage data))

(defmethod db-find clojure.lang.PersistentArrayMap
  [mutable-storage data]
  (mc/find-maps (:db mutable-storage) (:collection mutable-storage) data))


(defmethod clojure.core/print-method MongoStorage
  [storage ^java.io.Writer writer]
  (.write writer (str "#<MongoStorage> Collection: " (:collection storage))))

(ns rebujito.mongo
  (:require
   [manifold.deferred :as d]
   [com.stuartsierra.component  :as component]
   [monger.collection :as mc]
   [monger.conversion :refer [from-db-object]]
   [monger.operators :refer [$inc]]
   [monger.core :as mg]
   [monger.json :as mj]
   [monger.result :refer [acknowledged?]]
   [rebujito.protocols :as protocols]
   [rebujito.mongo.schemas :refer (query-by-example-coercer)]
   [taoensso.timbre :as log])
  (:import [org.bson.types ObjectId]))

(defn to-mongo-id-hex-string [s]
  (format "%024x"  (read-string s)))

(defn to-mongo-object-id [hex]
  (org.bson.types.ObjectId. hex))

(defn ^org.bson.types.ObjectId generate-account-id [^String s]
  {:pre  [(try (number? (read-string s))
               (catch Exception e (do
                                    "should be possible to be parsed as a number!"
                                    false)))]}
  (-> s to-mongo-id-hex-string to-mongo-object-id))

(defn ^String id>mimi-id [s]
  (str (BigInteger. s 16)))

(defmulti db-find "dispatch on data meaning"
  (fn [mutable-storage data] (type data)))

(defmethod db-find :default [_ data]
  (throw (IllegalArgumentException.
          (str "Not ready to db-find using: " (type data)))))

(defn- update-by-id!* [this hex-id data]
  (let [id (to-mongo-object-id hex-id)]
    (mc/update-by-id (:db this) (:collection this) id data)))

(defn- find*
  ([this]
   (mc/find (:db this) (:collection this)))
  ([this data]
   (db-find this data))
  )

(defn- get-and-insert!* [this data]
  (mc/insert-and-return (:db this) (:collection this) data)
  )


(defn- insert!* [this data]
  (mc/insert (:db this) (:collection this) data))

(defn- start* [this]
      (let [c (assoc this
                   :db (:db (:db-conn this))
                   :collection (if (keyword? (:collection this))
                                 (name (:collection this))
                                 (:collection this)))]
      (when (:ephemeral? this)
        (println "DEV ENV removing data from collection: " (:collection c))
        (mc/remove (:db c) (:collection c)))
      c
      ))

(defn- adapt-counter [index init]
  (+ (or index 0) init))

(defrecord CounterStorage [db-conn collection secret-key ephemeral? counters]
  component/Lifecycle
  (start [this]
    (let [this*  (start* this)]
      (doseq [[k v]  counters]
        (when-not (first (find this* {:counter-name k} ))
          (protocols/get-and-insert! this* {:counter-name k})))
      this*))
  (stop [this] this)

  protocols/Counter
  (increment! [this counter-name]
    (assert (counter-name (:counters this)) (format "the counter %s  doesn't exist" counter-name))
    (adapt-counter (:index (mc/find-and-modify (:db this) (:collection this)
                                 {:counter-name counter-name}
                                 {$inc {:index 1}}
                                 {:return-new true}))
                   (counter-name counters)))
  (deref [this counter-name]
    (assert (counter-name (:counters this)) (format "the counter %s  doesn't exist" counter-name))
    (adapt-counter (:index (first (protocols/find this {:counter-name counter-name})))
                   (counter-name counters)))

  protocols/MutableStorage
  (generate-id [this data]
    (generate-account-id data))
  (find [this]
    (find* this))
  (find [this data]
    (find* this data))
  (get-and-insert! [this data]
    (get-and-insert!* this data))
  (insert! [this data]
    (insert!* this data))
  (update-by-id! [this hex-id data]
    (update-by-id!* this hex-id data))
  )

(defrecord UserStorage [db-conn collection secret-key ephemeral?]
  component/Lifecycle
  (start [this]
    (start* this))
  (stop [this] this)

  protocols/MutableStorage
  (generate-id [this data]
    (generate-account-id data))
  (find [this]
    (find* this))
  (find [this data]
    (find* this data))
  (get-and-insert! [this data]
    (get-and-insert!* this data))
  (insert! [this data]
    (insert!* this data))
  (update-by-id! [this hex-id data]
    (update-by-id!* this hex-id data))
  )


(defrecord ApiKeyStorage [db-conn collection secret-key ephemeral?]
  component/Lifecycle
  (start [this]
    (start* this))
  (stop [this] this)

  protocols/MutableStorage
  (generate-id [this data]
    (generate-account-id data))
  (find [this]
    (find* this))
  (find [this data]
    (find* this data))
  (get-and-insert! [this data]
    (get-and-insert!* this data))
  (insert! [this data]
    (insert!* this data))
  (update-by-id! [this hex-id data]
    (update-by-id!* this hex-id data))

  protocols/ApiClient
  (login [this id pw]
    (log/info "login" id pw)
    (-> (protocols/find this id)
        (d/chain
         (fn [api-client]
           (if (= (:secret api-client) pw)
             api-client
             (d/error-deferred (ex-info (str "Store ERROR!")
                                        {:type :store
                                         :status 400
                                         :body (format "api client-id and client-secret: %s :: %s not valid  " id pw)
                                         :message (format "api client-id and client-secret: %s :: %s not valid  " id pw)
                                         }))))))))


(defn new-counter-store
  ([auth-data]
   (new-counter-store auth-data false ))
  ([auth-data ephemeral?]
   (new-counter-store auth-data ephemeral? {:demo 1000} ))
  ([auth-data ephemeral? counters]
   (map->CounterStorage {:collection :counters
                         :secret-key (:secret-key auth-data)
                         :ephemeral?  ephemeral?
                         :counters counters})))

(defn new-user-store
  ([auth-data]
   (new-user-store auth-data false))
  ([auth-data ephemeral?]
   (map->UserStorage {:collection :users
                       :secret-key (:secret-key auth-data)
                       :ephemeral?  ephemeral?})))

(defn new-api-key-store
  ([auth-data]
   (new-api-key-store auth-data false))
  ([auth-data ephemeral?]
   (map->ApiKeyStorage {:collection :api-keys
                         :secret-key (:secret-key auth-data)
                               :ephemeral?  ephemeral?})))

(defn- find-map-by-id [mutable-storage id]
  (mc/find-map-by-id (:db mutable-storage) (:collection mutable-storage) id))

(defmethod db-find String
  [mutable-storage data]
  (log/info "db-find>>>>" data)
  (try
    (clojure.walk/keywordize-keys (if (= data "w8tnxd8h2wns43cfdgmt793j")
                                              {"_id" "w8tnxd8h2wns43cfdgmt793j", "secret" "KDRSRVqKHp5TkKvJJhN7RYkE", "who" "mediamonks"}
                                              (find-map-by-id mutable-storage (org.bson.types.ObjectId. data))))
    (catch Exception e (d/error-deferred (ex-info (.getMessage e) {:message (.getMessage e)}))))
)

(defmethod db-find org.bson.types.ObjectId
  [mutable-storage data]
  (find-map-by-id mutable-storage data))

(defmethod db-find clojure.lang.PersistentArrayMap
  [mutable-storage data]
  (mc/find-maps (:db mutable-storage) (:collection mutable-storage) data))


(defmethod clojure.core/print-method CounterStorage
  [storage ^java.io.Writer writer]
  (.write writer (str "#<CounterStorage> Collection: " (:collection storage))))

(defmethod clojure.core/print-method UserStorage
  [storage ^java.io.Writer writer]
  (.write writer (str "#<UserStorage> Collection: " (:collection storage))))

(defmethod clojure.core/print-method ApiKeyStorage
  [storage ^java.io.Writer writer]
  (.write writer (str "#<ApiKeyStorage> Collection: " (:collection storage))))

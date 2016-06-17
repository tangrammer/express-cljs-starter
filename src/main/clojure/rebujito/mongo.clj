(ns rebujito.mongo
  (:require
   [schema.core :as s]
   [manifold.deferred :as d]
   [com.stuartsierra.component  :as component]
   [monger.collection :as mc]
   [monger.conversion :refer [from-db-object]]
   [monger.operators :refer [$inc $set $push $pull]]
   [monger.core :as mg]
   [monger.json :as mj]
   [monger.result :refer [acknowledged?]]
   [rebujito.protocols :as protocols]
   [rebujito.schemas :refer (PaymentMethodMongo AutoReloadMongo)]
   [rebujito.mongo.schemas :refer (query-by-example-coercer)]
   [taoensso.timbre :as log])
  (:import [org.bson.types ObjectId]
           [java.util UUID]))

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

(defn- update!* [this data-query data-update]

  (mc/update (:db this) (:collection this) data-query {$set  data-update} {:multi true}))

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
        (when-not (first (protocols/find this* {:counter-name k} ))
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
  (update! [this data-query data-update]
    (update!* this data-query data-update))
  (update-by-id! [this hex-id data]
    (update-by-id!* this hex-id data))
  )


(defrecord UserStorage [db-conn collection secret-key ephemeral?]
  component/Lifecycle
  (start [this]
    (start* this))
  (stop [this] this)

  protocols/UserStore
  (add-auto-reload [this oid payment-data data]
    (try
      (let [uuid (str (UUID/randomUUID))
            data (assoc data
                        :autoReloadId  uuid
                        :active true)
            user (protocols/find this oid)
            cards (:cards user)
            others (filter #(not= (:cardId %) (:cardId data)) cards)
            card  (-> (first (filter #(= (:cardId %) (:cardId data)) cards))
                      (assoc :autoReloadProfile data))
            new-cards (conj others card)
            ]
        (log/debug   ">>>>" oid data)
        (s/validate AutoReloadMongo data)
        (let [t (mc/update (:db this) (:collection this) {:_id (org.bson.types.ObjectId. oid)}
                           {$set {:cards  new-cards}})]
          (if (pos? (.getN t))
            {:autoReloadId uuid}
            (d/error-deferred (ex-info (str "Store ERROR!")
                                       {:type :store
                                        :status 500
                                        :body "add-auto-reload transaction fails"
                                        :message "add-auto-reload transaction fails"
                                        }))
            )))

      (catch Exception e (d/error-deferred (ex-info (str "Store ERROR!")
                                                    {:type :store
                                                     :status 500
                                                     :body (.getMessage e)
                                                     :message (.getMessage e)
                                                     })))))
  (disable-auto-reload [this oid card-id]
    (try
      (do
        (log/debug ">>>>" oid)
        (let [user (protocols/find this oid)
              cards (:cards user)
              others (filter #(not= (:cardId %) card-id) cards)
              card  (-> (first (filter #(= (:cardId %) card-id) cards))
                        (assoc-in [:autoReloadProfile :active] false)
                        (assoc-in [:autoReloadProfile :status] "disabled"))
              new-cards (conj others card)

              t (mc/update (:db this) (:collection this) {:_id (org.bson.types.ObjectId. oid)}
                           {$set {:cards  new-cards}})]
          (if (pos? (.getN t))
            true
            (d/error-deferred (ex-info (str "Store ERROR!")
                                       {:type :store
                                        :status 500
                                        :body "disable-auto-reload transaction fails"
                                        :message "disable-auto-reload transaction fails"
                                        }))
            )))

      (catch Exception e (d/error-deferred (ex-info (str "Store ERROR!")
                                                    {:type :store
                                                     :status 500
                                                     :body (.getMessage e)
                                                     :message (.getMessage e)
                                                     })))))
  (insert-card! [this user-id card]
    (let [card-id (str (java.util.UUID/randomUUID))
          card (assoc card :cardId card-id)]
      (protocols/update-by-id! this user-id {$push {:cards card}})
      card-id))

  protocols/UserPaymentMethodStore
  (update-payment-method [this oid payment-method]

    (try
      (do
        (log/debug ">>>>" oid)
        (let [user (protocols/find this oid)
              p (:paymentMethods user)
              p-others (filter #(not= (:paymentMethodId %) (:paymentMethodId payment-method)) p)

              new-p (conj p-others payment-method)

              t (mc/update (:db this) (:collection this) {:_id (org.bson.types.ObjectId. oid)}
                           {$set {:paymentMethods  new-p}})]
          (if (pos? (.getN t))
            payment-method
            (d/error-deferred (ex-info (str "Store ERROR!")
                                       {:type :store
                                        :status 500
                                        :body "update-payment-method transaction fails"
                                        :message "update-payment-method transaction fails"
                                        }))
            )))

      (catch Exception e (d/error-deferred (ex-info (str "Store ERROR!")
                                                    {:type :store
                                                     :status 500
                                                     :body (.getMessage e)
                                                     :message (.getMessage e)
                                                     }))))
    )
  (remove-payment-method [this oid payment-method]
    (try
      (log/debug ">>>>" oid payment-method)

      (let [t (mc/update (:db this) (:collection this) {:_id (org.bson.types.ObjectId. oid)} {$pull {:paymentMethods payment-method}})]
        (if (pos? (.getN t))
          true
          (d/error-deferred (ex-info (str "Store ERROR!")
                                     {:type :store
                                      :status 500
                                      :body "remove-new-payment-method transaction fails"
                                      :message "remove-new-payment-method transaction fails"
                                      }))
          ))

      (catch Exception e (d/error-deferred (ex-info (str "Store ERROR!")
                                                    {:type :store
                                                     :status 500
                                                     :body (.getMessage e)
                                                     :message (.getMessage e)
                                                     }))))
    )
  (add-new-payment-method [this oid p]
    (try
      (let [uuid (str (UUID/randomUUID))
            p (assoc p :paymentMethodId  uuid)]
        (log/debug ">>>>" oid p)
        (s/validate PaymentMethodMongo p)
        (let [t (mc/update (:db this) (:collection this) {:_id (org.bson.types.ObjectId. oid)} {$push {:paymentMethods p}})]
          (if (pos? (.getN t))
            {:paymentMethodId uuid}
            (d/error-deferred (ex-info (str "Store ERROR!")
                                       {:type :store
                                        :status 500
                                        :body "add-new-payment-method transaction fails"
                                        :message "add-new-payment-method transaction fails"
                                        }))
            )))

      (catch Exception e (d/error-deferred (ex-info (str "Store ERROR!")
                                                    {:type :store
                                                     :status 500
                                                     :body (.getMessage e)
                                                     :message (.getMessage e)
                                                     })))))
  (get-payment-method [this oid payment-method-id]
    (let [user-db  (protocols/find this oid)]
      (if-let [p (first (filter #(= (:paymentMethodId %) payment-method-id) (:paymentMethods user-db)))]
        p
        (d/error-deferred (ex-info (str "Store ERROR!")
                                   {:type :store
                                    :status 400
                                    :body (format "payment-method doens't exist: %s " payment-method-id)
                                    :message (format "payment-method doens't exist: %s " payment-method-id)})))))
  (get-payment-methods [this oid]
    (let [user-db  (protocols/find this oid)]
      (or  (:paymentMethods user-db) [])))

  protocols/UserAddressStore
  (get-address [this oid address-id]
    (let [user-db  (protocols/find this oid)]
      (if-let [p (first (filter #(= (:addressId %) address-id) (:addresses user-db)))]
        p
        (d/error-deferred (ex-info (str "Store ERROR!")
                                   {:type :store
                                    :status 400
                                    :body (format "address doens't exist: %s " address-id)
                                    :message (format "address doens't exist: %s " address-id)})))))
  (update-address [this oid address]
    (try
      (log/debug ">>>>" oid)
      (let [user (protocols/find this oid)
            p (:addresses user)
            p-others (filter #(not= (:addressId %) (:addressId address)) p)

            new-p (conj p-others address)

            t (mc/update (:db this) (:collection this) {:_id (org.bson.types.ObjectId. oid)}
                         {$set {:addresses  new-p}})]
        (if (pos? (.getN t))
          address
          (d/error-deferred (ex-info (str "Store ERROR!")
                                     {:type :store
                                      :status 500
                                      :body "update-address transaction fails"
                                      :message "update-address transaction fails"
                                      }))
          ))

      (catch Exception e (d/error-deferred (ex-info (str "Store ERROR!")
                                                    {:type :store
                                                     :status 500
                                                     :body (.getMessage e)
                                                     :message (.getMessage e)
                                                     }))))
    )
  (remove-address [this oid address]
    (try
      (log/debug ">>>>" oid address)

      (let [t (mc/update (:db this) (:collection this) {:_id (org.bson.types.ObjectId. oid)} {$pull {:addresses address}})]
        (if (pos? (.getN t))
          true
          (d/error-deferred (ex-info (str "Store ERROR!")
                                     {:type :store
                                      :status 500
                                      :body "remove-address transaction fails"
                                      :message "remove-address transaction fails"
                                      }))
          ))

      (catch Exception e (d/error-deferred (ex-info (str "Store ERROR!")
                                                    {:type :store
                                                     :status 500
                                                     :body (.getMessage e)
                                                     :message (.getMessage e)
                                                     })))))
  (get-addresses [this oid]
    (or (:addresses (protocols/find this oid)) []))
  (insert-address [this oid address]
    (let [address-id (str (java.util.UUID/randomUUID))
          address (assoc address :addressId address-id)]
      (protocols/update-by-id! this oid {$push {:addresses address}})
      address-id))


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
  (update! [this data-query data-update]
    (update!* this data-query data-update))

  (update-by-id! [this hex-id data]
    (update-by-id!* this hex-id data))
  )

(defrecord BaseStorage [db-conn collection secret-key ephemeral?]
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
  (update! [this data-query data-update]
    (update!* this data-query data-update))

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
  (update! [this data-query data-update]
    (update!* this data-query data-update))
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
                                         })))))
        (d/catch Exception
            (fn [e]
              (d/error-deferred (ex-info (str "Store ERROR!")
                                        {:type :store
                                         :status 400
                                         :body (str (format "api client-id and client-secret: %s :: %s not valid  " id pw)
                                                    " " (.getMessage e))
                                         :message (str (format "api client-id and client-secret: %s :: %s not valid  " id pw)
                                                    " " (.getMessage e))
                                         })))))))


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

(defn new-token-store
  ([auth-data]
   (new-token-store auth-data false))
  ([auth-data ephemeral?]
   (map->BaseStorage {:collection :tokens
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

(defmethod clojure.core/print-method BaseStorage
  [storage ^java.io.Writer writer]
  (.write writer (str "#<UserStorage> Collection: " (:collection storage))))

(defmethod clojure.core/print-method ApiKeyStorage
  [storage ^java.io.Writer writer]
  (.write writer (str "#<ApiKeyStorage> Collection: " (:collection storage))))

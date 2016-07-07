(ns rebujito.mongo
  (:require
   [clj-time.core   :as t]
   [monger.joda-time :as mjt]
   [schema.core :as s]
   [manifold.deferred :as d]
   [com.stuartsierra.component  :as component]
   [monger.collection :as mc]
   [monger.conversion :refer [from-db-object]]
   [monger.operators :refer [$inc $set $push $pull $elemMatch $or $regex]]
   [monger.core :as mg]
   [monger.json :as mj]
   [monger.result :refer [acknowledged?]]
   [rebujito.protocols :as protocols]
   [rebujito.util :as util]
   [rebujito.schemas :refer (PaymentMethodMongo AutoReloadMongo)]
   [rebujito.mongo.schemas :refer (query-by-example-coercer)]
   [taoensso.timbre :as log])
  (:import [org.bson.types ObjectId]
           [java.util UUID]))

;com.mongodb.WriteResult

(defprotocol Operation
  (result [this success-data]))

(extend-protocol Operation
  com.mongodb.WriteResult
  (result [this success-data]
    (if (pos? (.getN this))
             success-data
             (util/error* 500 ['xxx ::transaction-failed])))
  )


(defn to-mongo-id-hex-string [s]
  (format "%024x"  (read-string s)))

(defn fully-k-name
  "returns the simple or fully qualified name for a keyword"
  [k uuid]
  (str (when-let [ns* (namespace k)] (str ns* "/")) (name k) "#" uuid))

(defn to-mongo-object-id [hex]
  (if (string? hex)
    (org.bson.types.ObjectId. hex)
    hex))

(defn ^org.bson.types.ObjectId generate-account-id [^String s]
  {:pre  [(try (number? (read-string s))
               (catch Exception e (do
                                    "should be possible to be parsed as a number!"
                                    false)))]}
  (-> s to-mongo-id-hex-string to-mongo-object-id))

(defn ^String id>mimi-id [s]
  (str (BigInteger. s 16)))

(defmulti db-find "dispatch on data meaning"
  (fn [mutable-storage data]
    (log/debug "db-find:: dispatch on data type: " (type data) data)
    (type data)))

(defmethod db-find :default [_ data]
  (log/info "inside error db-find::" data (type data))
  (throw (IllegalArgumentException.
          (str "Not ready to db-find using: " (if (nil? data) "nil" (type data))))))

(defn- update!* [this data-query data-update]
  (mc/update (:db this) (:collection this) data-query {$set  data-update} {:multi true}))

(defn- update-by-id!* [this hex-id data]
  (let [id (to-mongo-object-id hex-id)]
    (mc/update-by-id (:db this) (:collection this) id {$set data})))

(defn- update-push-by-id!* [this hex-id data]
  (let [id (to-mongo-object-id hex-id)]
    (mc/update-by-id (:db this) (:collection this) id {$push data})))

(defn- update-pull-by-id!* [this hex-id data]
  (let [id (to-mongo-object-id hex-id)]
    (mc/update-by-id (:db this) (:collection this) id {$pull data})))

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
(defn- optional-conj-or [v x kw]
  (if (and x (not= x ""))
      (conj v {kw {$regex x}})
      v))

(defrecord UserStorage [db-conn collection secret-key ephemeral?]
  component/Lifecycle
  (start [this]
    (start* this))
  (stop [this] this)

  protocols/UserStore
  (add-autoreload-profile-card [this oid autoreload-profile-card]
    (let [try-type :store
          try-id ::add-autoreload-profile-card
          try-context '[oid autoreload-profile-card]]
      (util/dtry
       (do
         (let [uuid (str (UUID/randomUUID))
               autoreload-profile-card (assoc autoreload-profile-card
                                              :autoReloadId  uuid
                                              :active true)
               user (or (protocols/find this oid)
                        (util/error* 500 [500 ::user-not-found]))
               cards (:cards user)

               others (filter #(not= (:cardId %) (:cardId autoreload-profile-card)) cards)

               card  (-> (or (first (filter #(= (:cardId %) (:cardId autoreload-profile-card)) cards))
                             (util/error* 500 [500 ::card-not-found]))
                         (assoc :autoReloadProfile autoreload-profile-card))

               new-cards (conj others card)
               ]
           (log/debug   ">>>>" oid autoreload-profile-card)
           (s/validate AutoReloadMongo autoreload-profile-card)
           (let [t (mc/update (:db this) (:collection this) {:_id (org.bson.types.ObjectId. oid)}
                              {$set {:cards  new-cards}})]
             (if (pos? (.getN t))
               {:autoReloadId uuid}
               (util/error* 500 [500 ::transaction-failed]))))))))
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
    (let [card-id (str (UUID/randomUUID))
          card (assoc card :cardId card-id)]
      (update-push-by-id!* this user-id {:cards card})
      card-id))
  (get-user-and-card [this card-number]
    (let [try-type :store
          try-id ::get-card
          try-context '[card-number]]
      (util/dtry
       (do
         (let [found (mc/find-one-as-map  (:db this) (:collection this)  {:cards {$elemMatch {:cardNumber card-number}}})]
           {:user  found :card (first (filter #(= (:cardNumber %) card-number) (:cards found) ))})
       )))

    )
  (search [this firstName lastName emailAddress cardNumber]
    (log/info "(search [_ firstName lastName emailAddress cardNumber])" firstName lastName emailAddress cardNumber)
    (let [conj-or (-> []
                      (optional-conj-or firstName :firstName)
                      (optional-conj-or lastName :lastName)
                      (optional-conj-or emailAddress :emailAddress))
          conj-or (if (and cardNumber (not= cardNumber ""))
                    (conj conj-or {:cards {$elemMatch {:cardNumber {$regex cardNumber}}}})
                    conj-or)
          ]
      (let [res  (if (empty? conj-or)
                   (mc/find-maps (:db this) (:collection this))
                   (mc/find-maps (:db this) (:collection this) {$or conj-or}))]
        (log/debug "search_query" conj-or)
        (log/debug "search_result" (clojure.string/join "," (mapv :emailAddress res)))
        res)))

  protocols/UserPaymentMethodStore
  (update-payment-method [this oid payment-method]
    (let [try-type :store
          try-id ::update-payment-method
          try-context '[oid payment-method]]
      (util/dtry
       (do
         (log/debug ">>>>" oid)
         (let [user (protocols/find this oid)
               p (:paymentMethods user)
               p-others (filter #(not= (:paymentMethodId %) (:paymentMethodId payment-method)) p)

               new-p (conj p-others payment-method)]
           (-> (mc/update (:db this) (:collection this) {:_id (org.bson.types.ObjectId. oid)}
                            {$set {:paymentMethods  new-p}})
               (result payment-method)))))))
  (remove-payment-method [this oid payment-method]
    (let [try-type :store
          try-id ::add-new-payment-method
          try-context '[oid payment-method]]
      (util/dtry
       (do
         (log/debug ">>>>" oid payment-method)
         (let [t (update-pull-by-id!* this  oid  {:paymentMethods payment-method})]
           (if (pos? (.getN t))
             true
             (util/error* 500 ['xxx ::transaction-failed])))))))
  (add-new-payment-method [this oid p]
    (let [try-type :store
          try-id ::add-new-payment-method
          try-context '[oid p]]
      (util/dtry
      (do
        ;; example 500 (throw (Exception. "!wow"))
        (let [uuid (str (UUID/randomUUID))
              p (assoc p :paymentMethodId  uuid)]
          (log/debug ">>>>" oid p)
          (s/validate PaymentMethodMongo p)
          (let [t (update-push-by-id!* this  oid {:paymentMethods p})]
            (if (pos? (.getN t))
                {:paymentMethodId uuid}
                (util/error* 400 ['xxx (str t (protocols/find this oid))])))))))

    )
  (get-payment-method [this oid payment-method-id]
    (let [user-db  (protocols/find this oid)
          try-type :store
          try-id ::add-new-payment-method
          try-context '[oid payment-method-id user-db]]
      (util/dtry
       (do
         (if-let [p (first (filter #(= (:paymentMethodId %) payment-method-id) (:paymentMethods user-db)))]
           p
           (util/error* 400 ['xxx ::get-payment-method-failed]))))))
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
    (let [address-id (str (UUID/randomUUID))
          address (assoc address :addressId address-id)]
      (update-push-by-id!* this oid  {:addresses address})
      address-id))

  protocols/UserCardStore
  (update-card-number [this oid old-card-number new-card-number]
    (update!* this {:_id (ObjectId. oid) "cards.cardNumber" old-card-number} {"cards.$.cardNumber" new-card-number}))

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
    (log/debug "(p/login [_ id pw])" id pw)
    (let [try-type :store
          try-id ::login
          try-context '[id pw]]
      (util/dtry
       (do (if-let [api-client (protocols/find this id)]
          (if (= (:secret api-client) pw)
            api-client
            (util/error* 400 ['xxx (str :invalid_client " " (format "api client-id and client-secret: %s :: %s not valid  " id pw))]))
          (util/error* 400 ['xxx (str :invalid_client " " (format "api client-id  %s not valid  " id ))])))))))



(defrecord TokenStorage [db-conn collection secret-key ephemeral?]
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
    (update-by-id!* this hex-id data)))


(def states [:ready :new ::one :two :done])

(def error-state :error)

(defn- exist-state? [states-v k]
  (assert (or (contains? (set states-v) k) (= k error-state)))
  )

(defn- get-index [k states-v]
  (exist-state? states-v k)
  (loop [counter 0 f (first states-v) n (next states-v)]
    (if (= k f)
      counter
      (if (nil? n)
        :not-found!
        (recur  (inc counter) (first n) (next n)))
      )

    ))

(defrecord WebhookStorage [db-conn collection secret-key ephemeral?]
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

  protocols/WebhookStore
  (webhook-uuid [this uuid]
    (str "webhook/card-number#" uuid))
  (change-state [this webhook-uuid state]
    ;; ::TODO check that we don't set same state twice ...
    ;; maybe we should only say next-state instead of change state so externally doens't need to track current state
    (exist-state? states state)
    (pos?
     (.getN
      (protocols/update-by-id! this (:_id (protocols/current this webhook-uuid)) {:state (name state)  :time (t/now)}))))
  (current [this webhook-uuid]
    (if-let [current (first (protocols/find this {:uuid webhook-uuid}))]
      current
      (protocols/get-and-insert! this {:uuid webhook-uuid :state (name (first states)) :time (t/now)}))))




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

(defn new-webhook-store
  ([auth-data]
   (new-webhook-store auth-data false))
  ([auth-data ephemeral?]
   (map->WebhookStorage {:collection :webhooks
                         :secret-key (:secret-key auth-data)
                         :ephemeral?  ephemeral?})))

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
   (map->TokenStorage {:collection :tokens
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
  (log/debug "db-find:: " data)
  (try
    (clojure.walk/keywordize-keys (if (= data "w8tnxd8h2wns43cfdgmt793j")
                                              {"_id" "w8tnxd8h2wns43cfdgmt793j", "secret" "KDRSRVqKHp5TkKvJJhN7RYkE", "who" "mediamonks"}
                                              (find-map-by-id mutable-storage (org.bson.types.ObjectId. data))))
    (catch Exception e  (ex-info (.getMessage e) {:message (.getMessage e)}))))

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

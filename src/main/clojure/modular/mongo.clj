;; full component version  https://github.com/danielsz/system/tree/master/src/system/components but hopefully simplified following https://github.com/juxt/modular way
;; should be promoted to juxt/modular when is ready so we'll have juxt and community support
(ns modular.mongo
  (:require
   [schema.core :as s]
   [com.stuartsierra.component :as component]
   [monger.core :as mg])
  (:import [com.mongodb MongoOptions ServerAddress]))

(defrecord MongoDatabase []
  component/Lifecycle
  (start [this] this)
  (stop [this] this))

(def UserSchema
  {:username s/Str
   :password s/Str})

(def DatabaseSchema
  {:host s/Str
   :port s/Int
   (s/optional-key :database) s/Str
   (s/optional-key :user) UserSchema})

(defn new-mongo-database [opts]
  (let [opts (->> opts
                  (merge {:host "localhost"
                          :port 27017
                          :database "documents"}))]
    (s/validate DatabaseSchema opts))
  (map->MongoDatabase opts))

(defrecord MongoConnection [database]
  component/Lifecycle
  (start [this]
    (let [conn (mg/connect (:database this))]
      (assoc this
             :connection conn
             :db (mg/get-db conn (-> this :database :database)))))
  (stop [this]
    (when (:connection this)
      (mg/disconnect (:connection this)))
    (dissoc this :connection)))

(defn new-mongo-connection []
  (map->MongoConnection {}))

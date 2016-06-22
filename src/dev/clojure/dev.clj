(ns dev
  "Tools for interactive development with the REPL. This file should
  not be included in a production build of the application."
  (:import [java.util Locale])
  (:require
   [bidi.bidi :as bidi]
   [buddy.core.codecs :refer (bytes->hex)]
   [buddy.core.nonce :as nonce]
   [clojure.java.browse :refer (browse-url)]
   [clojure.java.io :as io]
   [clojure.java.javadoc :refer [javadoc]]
   [clojure.pprint :refer [pprint pp]]
   [clojure.reflect :refer [reflect]]
   [clojure.repl :refer [apropos dir doc find-doc pst source]]
   [clojure.test :refer [run-tests run-all-tests]]
   [clojure.tools.namespace.repl :refer [refresh refresh-all]]
   [com.stuartsierra.component :refer [system-map system-using using] :as component]
   [frak :as frak]
   [manifold.deferred :as d]
   [manifold.stream :as s]
   [monger.collection :as mc]
   [monger.operators :refer [$inc $set $push]]
   [monger.core :as mg]
   [plumbing.core :refer (fnk defnk)]
   [rebujito.api.resources.account :as ac]
   [rebujito.api.sig :as sig]
   [rebujito.base-test :refer (new-account-sb check-monks-api-key)]
   [rebujito.config :refer [config]]
   [rebujito.logging :as log-levels]
   [rebujito.protocols :as p]
   [rebujito.system.dev-system :refer (new-dev-system)]
   [schema-generators.generators :as g]
   [schema.core :as schema]
   [taoensso.timbre :as log :refer (trace debug info warn error)]
    env
    ))


;; reloaded fns

(declare check-mobile-user )

(defonce
  ^{:docstring "A Var containing an object representing the application under development."}
  system
  nil)

(defn alter-system!
  [f & args]
  (apply alter-var-root #'system f args))

(defn set-system!
  [val]
  (alter-system! (constantly val)))

(defn start-system! []
  (schema/with-fn-validation
    (alter-system! component/start)))

(defn stop-system! []
  (alter-system! (fn [s] (when s (component/stop s)))))

(defn init
  "Creates and initializes the system under development in the Var
  #'system."
  ([]
   (init env/env))
  ([env]
   (set-system! (new-dev-system env))))

(defn start
  "Starts the system running, updates the Var #'system."
  []
  (start-system!))

(defn stop
  "Stops the system if it is currently running, updates the Var
  #'system."
  []
  (stop-system!))

(defn dont-send-bugsnag []
  (alter-var-root (var rebujito.util/*send-bugsnag*) (fn [d] false)))

(defn go
  "Initializes and starts the system running."
  []
  (log/set-config! (assoc log-levels/timbre-info-config
                          :level env/log-level))
  (init)
  (dont-send-bugsnag)
  (start)
  (check-mobile-user)
  (check-monks-api-key system)
  :ready)

(defn reset
  "Stops the system, reloads modified source files, and restarts it."
  []
  (stop)
  (refresh :after `go))

;; validation
(defmacro validate [& body]
  `(schema/with-fn-validation ~@body))

;; open swagger in browser
(defn open-app
  ([]
   (open-app :dev))
  ([env]
   (assert (contains? #{:dev :test :prod} env))
   (browse-url (format "http://%s/swagger-ui/index.html?url=/starbucks/v1/swagger.json"
                       (condp = env
                         :dev (str "localhost:" (-> (config env) :yada :port)))))))

(defn set-env! [& env]
  (alter-var-root #'env/env (constantly (set env))))

(defn set-log-level! [level]
  (alter-var-root #'env/log-level level))

(defn generate-random [n]
  (-> (nonce/random-bytes n)
      (bytes->hex)))

(defn insert [data]
  (p/insert! (-> system :api-client-store) data))

(defn find* [id]
  (p/find (-> system :api-client-store) id))

(defn find-all
  ([]
   (find-all :api-client-store))
  ([store-kw]
   (p/find (-> system store-kw))))

(defn check-mobile-user []
  (try
    (let [monks-user (:user (:monks (config :test)))
          u (-> (new-account-sb)
               (merge
                {:emailAddress (:email monks-user) :password (:pw monks-user)}))]
     (if-let [res (first (p/find (-> system :user-store) (select-keys u [:emailAddress]) ))]
       (println (str "default mobile user exists in db"  #_res))
       (do (ac/create-account-mongo! u [(str (rand-int 1000000))] (-> system :user-store) (:crypto system))
           (println "inserting default mobile user in db => " u))))
    (catch Exception e (do
                         (println (type  e) (.getMessage e))
                         (.printStackTrace e)
                         (pst e)
                         (check-mobile-user)))))

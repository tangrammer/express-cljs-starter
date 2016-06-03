(ns dev
  "Tools for interactive development with the REPL. This file should
  not be included in a production build of the application."
  (:import [java.util Locale])
  (:require
   [manifold.stream :as s]
   [rebujito.api.resources.account :as ac]
   [schema-generators.generators :as g]
   [rebujito.api.sig :as sig]
   [buddy.core.nonce :as nonce]
   [rebujito.protocols :as p]
   [buddy.core.codecs :refer (bytes->hex)]
    [clojure.java.browse :refer (browse-url)]
    [com.stuartsierra.component :refer [system-map system-using using] :as component]
    [clojure.java.io :as io]
    [clojure.java.javadoc :refer [javadoc]]
    [clojure.pprint :refer [pprint]]
    [clojure.reflect :refer [reflect]]
    [clojure.repl :refer [apropos dir doc find-doc pst source]]
    [clojure.test :refer [run-tests run-all-tests]]
    [clojure.tools.namespace.repl :refer [refresh refresh-all]]
    [rebujito.system.dev-system :refer (new-dev-system)]
    [manifold.deferred :as d]
    [plumbing.core :refer (fnk defnk)]
    [bidi.bidi :as bidi]
    [schema.core :as schema]
    [taoensso.timbre :as log :refer (trace debug info warn error)]
    [rebujito.config :refer [config]]
    [monger.core :as mg]
    [monger.collection :as mc]
    env))


;; reloaded fns

(declare check-mobile-user)

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

(defn go
  "Initializes and starts the system running."
  []
  (init)
  (start)
  (check-mobile-user)
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
(defn generate-random [n]
  (-> (nonce/random-bytes n)
      (bytes->hex)
      )
  )

(generate-random 32)


(defn insert-new-api-key [who]
  (p/get-and-insert! (-> system :api-client-store) {"secret" (generate-random 32) "who" who}))

(defn insert [data]
  (p/insert! (-> system :api-client-store) data)
  )

(defn find* [id]
  (p/find (-> system :api-client-store) id)
  )

(comment )

(defn find-all
  ([]
   (find-all :api-client-store))
  ([store-kw]
   (p/find (-> system store-kw))))


(defn check-mobile-user []
  (try
    (let [u (-> (g/generate (ac/schema :post))
               (merge
                {:emailAddress "stephan+starbucks_fr_02@mediamonks.com" :password "#myPassword01"}))]
     (if-let [res (first (p/find (-> system :user-store) (select-keys u [:emailAddress]) ))]
       (println (str "default mobile user exists in db"  #_res))
       (do ((ac/create-account-mongo! u (-> system :user-store) (:crypto system)) (-> (str (rand-int 1000000))
                                                                                      vector
                                                                                      (conj :mock-mimi)))
           (println "inserting default mobile user in db => " u)))
     )
    (catch Exception e (do (println (.getMessage e))
                           (check-mobile-user)
                           ))
    )
  )

(ns dev
  "Tools for interactive development with the REPL. This file should
  not be included in a production build of the application."
  (:import [java.util Locale])
  (:require
    [clojure.java.browse :refer (browse-url)]
    [com.stuartsierra.component :refer [system-map system-using using] :as component]
    [clojure.java.io :as io]
    [clojure.java.javadoc :refer [javadoc]]
    [clojure.pprint :refer [pprint]]
    [clojure.reflect :refer [reflect]]
    [clojure.repl :refer [apropos dir doc find-doc pst source]]
    [clojure.test :refer [run-tests run-all-tests]]
    [clojure.tools.namespace.repl :refer [refresh refresh-all]]
    [dev-system :refer (new-dev-system)]
    [environ.core :as environ]
    [plumbing.core :refer (fnk defnk)]
    [schema.core :as s]
    [taoensso.timbre :as log :refer (trace debug info warn error)]
    ))

;; reloaded fns

(def dev-system-options (atom #{}))
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
  (s/with-fn-validation
    (alter-system! component/start)))

(defn stop-system! []
  (alter-system! (fn [s] (when s (component/stop s)))))

(defn init
  "Creates and initializes the system under development in the Var
  #'system."
  []
;  (log/set-config! timbre-info-config)
  (set-system! (new-dev-system @dev-system-options)))

(defn start
  "Starts the system running, updates the Var #'system."
  []
  (start-system!)
  )

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
  :ready)

(defn reset
  "Stops the system, reloads modified source files, and restarts it."
  []
  (stop)
  (refresh :after `go))

;; validation
(defmacro validate [& body]
  `(s/with-fn-validation ~@body))

;; open swagger in browser
(defn open-app
  ([]
   (open-app :local))
  ([env]
   (assert (contains? #{:local :test :sam} env))
   (browse-url (format "http://%s/swagger-ui/index.html?url=/api/v1/swagger.json"
                       (condp = env
                         :local (str "localhost:" (environ/env :rebujito-yada-port)))))))

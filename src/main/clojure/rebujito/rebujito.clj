(ns rebujito.rebujito
  "Generated on lein uberjar"
  (:require
   [rebujito.system :refer (new-production-system)]
   [rebujito.config :refer (config)]
            [clojure.string :as str]
            [com.stuartsierra.component :refer (start)]
            [modular.ring :refer (request-handler)]

            [taoensso.timbre :as log ])
  (:gen-class))


(defn -main []
  ;; We eval so that we don't AOT anything beyond this class
  (log/info "Starting Rebujito!")
  (log/info "Env:" (with-out-str
                     (clojure.pprint/pprint
                      (select-keys (config :prod) [:env-type]))))
  (let [system (-> (new-production-system)
                   start)]
    (log/info "System starting")
    (log/info "This is your webserver port: " (-> system :webserver :port))
    (log/info ":) > Congrats, your system is ready to use!")
    system))

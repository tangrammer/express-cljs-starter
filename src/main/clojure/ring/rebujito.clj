(ns ring.rebujito
  "Generated on lein uberjar"
  (:require
   [com.stuartsierra.component :as component]
   [rebujito.config :refer (config)]
   [rebujito.system :refer (new-production-system)]
   [taoensso.timbre :as log])
  (:gen-class))

(def p (promise))

(defn -main []
  ;; We eval so that we don't AOT anything beyond this class

  (log/info "Env:" (with-out-str
                     (clojure.pprint/pprint
                      (select-keys (config :prod) [:env-type]))))
  (let [system (component/start (new-production-system))]
    (log/info "System starting")
    (log/info "This is your webserver port: " (-> system :webserver :port))
    (log/info ":) > Congrats, your system is ready to use!")
    system
    @p
    ))

(ns rebujito.mailer
  (:require
   [taoensso.timbre :as log]
   [rebujito.protocols :as protocols]
   [com.stuartsierra.component  :as component]))




(defrecord ProdMailer []
  component/Lifecycle
  (start [this]
    this)
  (stop [this] this)
  protocols/MailService
  (send [this data]
    (log/info "PROD: sending mail with this data" data)
    true
    ))


(defrecord MockMailer []
  component/Lifecycle
  (start [this]
    this)
  (stop [this] this)
  protocols/MailService
  (send [this data]
    (log/info "MOCK : sending mail with this data" data)
    true))




(defn new-prod-mailer [config]
  (map->ProdMailer config))

(defn new-mock-mailer [config]
  (map->MockMailer config))

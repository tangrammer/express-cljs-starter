(ns rebujito.mailer
  (:require
   [schema.core :as s]
   [rebujito.util :refer (dtry ddtry)]
   [byte-streams :as bs]
   [cheshire.core :as json]
   [rebujito.util :refer (error* dtry)]
   [aleph.http :as http]
   [taoensso.timbre :as log]
   [rebujito.protocols :as protocols]
   [com.stuartsierra.component  :as component]))

(def Mail {:subject String
           :to String
           :from String
           :content String})

(defrecord SendGridMailer [config]
  component/Lifecycle
  (start [this]
    this)
  (stop [this] this)
  protocols/MailService
  (send [this data]
    (log/info "PROD: sending mail with this data" data config)
    (let [data (merge {:from (:from  config)} data)
          try-id ::send
          try-type :mail
          try-context '[data]]
      (log/info data config)
      (dtry
       (do
         (s/validate Mail data)
         (let [res @(http/post (-> config :api :url)
                              {:throw-exceptions false
                               :body-encoding "UTF-8"
                               :headers {"Authorization" (format "Bearer %s" (-> config :api :token))}
                               :body (json/generate-string
                                      {:personalizations [{:to [{:email (:to data)}]}],
                                       :from {:email (:from data)},
                                       :subject (:subject data)
                                       :content [{:type "text/plain", :value (:content data)}]})
                               :content-type :json})]
          (if (= 202 (:status res))
            true
            (error* 500 [500 (-> res :body bs/to-string (json/parse-string true))]))))))))

(defrecord MockMailer [config]
  component/Lifecycle
  (start [this]
    (assoc this :mails (atom [])))
  (stop [this] this)
  protocols/MailService
  (send [this data]
    (log/info "MockMailer: sending mail with this data" data)
    (swap! (:mails this) conj data)
    true))

(defn new-sendgrid-mailer [config]
  (map->SendGridMailer {:config config}))

(defn new-mock-mailer [config]
  (map->MockMailer config))

(ns rebujito.mailer
  (:require
   [schema.core :as s]
   [rebujito.util :refer (dtry ddtry)]
   [byte-streams :as bs]
   [cheshire.core :as json]
   [rebujito.util :refer (error* dtry)]
   [org.httpkit.client :as http]
   ;[aleph.http :as http]
   [taoensso.timbre :as log]
   [rebujito.protocols :as protocols]
   [com.stuartsierra.component  :as component]))

(def Mail {:subject String
           :to (s/conditional string? String :else [String])
           :from String
           :content String
           (s/optional-key :content-type) (s/enum "text/plain" "text/html")})

(s/validate Mail {:to ["hoal"] :subject "asfd"  :from "asda" :content "asfda"} )
(s/validate Mail {:to ["hoal"] :subject "asfd"  :from "asda" :content "asfda" :content-type "text/html"} )

(defn- generate-to [to]
  (if (= (type to) clojure.lang.PersistentVector)
    (mapv #(hash-map :email %) to)
    [{:email to}]))

(generate-to ["hoa" "ueue"])

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
          try-context '[data config]]
      (log/info data config)
      (dtry
       (do
         (s/validate Mail data)
         (let [res @(http/post (-> config :api :url)
                              {:throw-exceptions false
                               :body-encoding "UTF-8"
                               :headers {"Authorization" (format "Bearer %s" (-> config :api :token))
                                         "Content-Type" "application/json"}
                               :body (json/generate-string
                                      {:personalizations [{:to (generate-to (:to data))}],
                                       :from {:email (:from data)},
                                       :subject (:subject data)
                                       :content [{:type (or (:content-type data) "text/plain")
                                                  :value (:content data)}]})})]
          (if (= 202 (:status res))
            true
            (error* 500 [500 (-> res :body bs/to-string )]))))))))

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

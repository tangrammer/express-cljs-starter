(ns rebujito.mimi
  (:require
   [rebujito.protocols :as protocols]
   [clj-http.client :as http-c]
   [org.httpkit.client :as http]
   [cheshire.core :as json]
   [byte-streams :as bs]
   [com.stuartsierra.component  :as component]
   [rebujito.store.mocks :as mocks]))

(def CreateAccountSchema
  {
   :address String
   :birthday String ;; 'YYYY-MM-DD'
   :city String
   :country String
   :email String
   :firstname String
   :gender String ;; (male|female)
   :lastname String
   :mobile String
   :password String
   :postalcode String
   :region String
   })

(defrecord ProdMimi [base-url token]
  component/Lifecycle
  (start [this]
    this)
  (stop [this] this)
  protocols/Mimi
  (create-account [this data]
    (println (format "%s/account" base-url))
    (println {"Authorization" (format "Bearer %s" token)})
    (let [{:keys [status body]}
          (http-c/post (format "%s/account" base-url)
                       {:headers {"Authorization" (format "Bearer %s" token)}
                        :insecure? true
                        :content-type :json
                        :accept :json
                        :as :json
                        :throw-exceptions true
                        :form-params {:firstname "Juan A."
                                      :lastname "Ruz"
                                      :password "xxxxxx"
                                      :email "juanantonioruz@gmail.com"
                                      :mobile "0034630051897"
                                      :city "Sevilla"
                                      :region "Andalucia"
                                      :postalcode "41003"
                                      :gender "male" ; (male|female)
                                      :birth {:dayOfMonth  "01"
                                              :month       "01"}}})]
      [status (-> body
                  :customerId
                  vector
                  (conj :prod-mimi))])))


(defrecord MockMimi [base-url token]
  component/Lifecycle
  (start [this]
    this)
  (stop [this] this)
  protocols/Mimi
  (create-account [this data ]
;;     (let [d* (d/deferred)]
;;       (d/success! d* ["200"  ["1000" #_(str (rand-int 1000))] :mock-mimi "Resource Created"])
;; ;      (throw (Exception. "jor"))
;; ;      (d/success! d* ["111046" ""])
;;       d*)
    ["200"  [(str (rand-int 1000))] :mock-mimi "Resource Created"]))

(defn new-prod-mimi [mimi-config]
  (map->ProdMimi mimi-config))

(defn new-mock-mimi [mimi-config]
  (map->MockMimi  mimi-config))

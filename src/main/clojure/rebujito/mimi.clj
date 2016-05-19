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
                        :form-params {:firstname "Juan A."
                                      :lastname "Ruz"
                                      :password "xxxxxx"
                                      :email "juanantonioruz@gmail.com"
                                      :mobile "0034630051897"
                                      :address "C/Antonio Pantion"
                                      :city "Sevilla"
                                      :region "Andalucia"
                                      :country "Spain"
                                      :postalcode "41003"
                                      :gender "male" ; (male|female)
                                      :birthday "1976-06-13" ; 'YYYY-MM-DD'
                                      }})]
      [status (-> body
                  first
                  vector
                  (conj :prod-mimi))])))


(defrecord MockMimi [base-url token]
  component/Lifecycle
  (start [this]
    this)
  (stop [this] this)
  protocols/Mimi
  (create-account [this data]
    ["200" ["1111" :mock-mimi "Resource Created"]]))

(defn new-prod-mimi [mimi-config]
  (map->ProdMimi mimi-config))

(defn new-mock-mimi [mimi-config]
  (map->MockMimi  mimi-config))

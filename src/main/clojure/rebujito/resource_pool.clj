(ns rebujito.resource-pool
  (:require
    [manifold.deferred :as d]
    [rebujito.protocols :as protocols]
    [clj-http.client :as http-c]
    [cheshire.core :as json]
    [com.stuartsierra.component  :as component]
    [schema.core :as s]
    [rebujito.util :refer [ddtry derror*]]
    [taoensso.timbre :as log]))

(defn call-resource-pool
  ([token url-method] (call-resource-pool token url-method {}))
  ([token url-method data]
   (http-c/request (merge url-method
                          {:headers {"Authorization" (format "Bearer %s" token)}
                           :form-params data
                           :insecure? true
                           :content-type :json
                           :accept :json
                           :as :json
                           :throw-exceptions false
                           }))))

(defrecord ProdCardResourcePool [base-url token]

  component/Lifecycle
  (start [this] this)
  (stop [this] this)

  protocols/CardResourcePool
  (checkout-physical-card [this card-number pin]
    (log/info "checkout-physical-card" card-number pin)
    (let [d* (d/deferred)]
      (d/future
        (let [url-method {:url "https://swarm-sandbox-2.appspot.com/pool/dev/starbucks/cards/physical"
                          ; :url "http://localhost:3001/pool/dev/starbucks/cards/physical"
                          :method :post}
              try-type :resource-pool
              try-id ::cheackout-physical-card
              try-context '[url card-number pin]]
          (ddtry d* (do
                      ;(s/validate CreateAccountSchema data)
                      (let [{:keys [status body]} (call-resource-pool token url-method {:cardNumber card-number :pin pin})]
                        (log/info "checkout-physical-card response" status body)
                        (condp = status
                           200 body
                           400 (derror* d* 400 [400 "can't validate card number and pin"])
                           403 (derror* d* 400 [400 "invalid pin"])
                           404 (derror* d* 400 [400 "card not found"])
                           410 (derror* d* 400 [400 "card has been used"])
                           (derror* d* 500 [500 "server error"]))
                        ))
                 )))
      d*))

  #_(checkout-physical-card [this card-number pin]
    (log/info "checkout-physical-card" card-number pin)
    (http-c/request (merge {:url "http://localhost:3001/starbucks/test/physical-card"
                            :method :delete}

                           {:headers {"Authorization" (format "Bearer %s" token)}
                            :form-params {:cardNumber card-number
                                          :pin pin}
                            :insecure? true
                            :content-type :json
                            :accept :json
                            :as :json
                            :throw-exceptions true
                            }))
    #_(d/future
      (Thread/sleep 100)
      #_(throw (ex-info "pin doesn't match" {:status 409 :body {:code "400" :message "pin doesn't match"}}))
      {:ok :ok}
      ))
    )

(defn new-prod-card-resource-pool [config]
  (map->ProdCardResourcePool config))

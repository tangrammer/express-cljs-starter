(ns rebujito.api.resources.card
  (:require
   [taoensso.timbre :as log]
   [manifold.deferred :as d]
   [rebujito.api.resources :refer (domain-exception)]
   [rebujito.protocols :as p]
   [rebujito.schemas :refer (AutoReloadMongo)]
   [rebujito.api.util :as util]
   [rebujito.mongo :refer [id>mimi-id]]
   [rebujito.store.mocks :as mocks]
   [monger.operators :refer [$push]]
   [cheshire.core :as json]
   [schema.core :as s]
   [yada.resource :refer [resource]]))

(def schema {:register-physical {:post {:cardNumber String}
                                 :pin String}
             :reload {:post {:amount Long
                             :paymentMethodId String
                             (s/optional-key :risk) s/Any
                             (s/optional-key :acceptTerms) Boolean
                             (s/optional-key :expirationYear) Long
                             (s/optional-key :expirationMonth) Long
                             (s/optional-key :sessionId) String}}
             :autoreload {:post {:status (s/enum "active" "disabled")
                                 :autoReloadType (s/enum "Date" "Amount")
                                 :day s/Num
                                 :triggerAmount s/Num
                                 :amount s/Num
                                 :paymentMethodId String}}})


(defn get-next-card-number [counter-store]
  (str (p/increment! counter-store :digital-card-number)))

(defn insert-card! [user-store user-id card]
  (let [card-id (str (java.util.UUID/randomUUID))
        card (assoc card :cardId card-id)]
    (p/update-by-id! user-store user-id {$push {:cards card}})
    card-id))

(defn new-physical-card [data]
  (merge data {:digital false}))

(defn new-digital-card [data]
  (merge data {:digital true}))

(defn dummy-card-data []
  {:primary true
   :cardCurrency "ZAR"
   :nickname "My Card"
   :type "Standard"
   :actions ["Reload"]
   :submarketCode "ZA"
   :balanceDate (.toString (java.time.Instant/now))
   :balanceCurrencyCode "ZAR"})

(def stored-value-program "Starbucks Card")

(defn get-points [mimi card-number]
  (d/let-flow [rewards (p/rewards mimi card-number)
               program (first (filter #(= (:program %) stored-value-program) (:programs rewards)))]
    (:balance program)))

(defn get-card [user-store user-id mimi]
  (d/let-flow [card-data (:cards (p/find user-store user-id))
               card-data (first card-data)
               balance (get-points mimi (:cardNumber card-data))]
    (when card-data
      (merge (dummy-card-data) card-data {:balance balance}))))

(defn cards [user-store mimi]
  (->
   {:methods
    {:get {:parameters {:query {:access_token String}}
           :response (fn [ctx]
                       (-> (d/let-flow [user-id (:_id (util/authenticated-user ctx))
                                        card-data (get-card user-store user-id mimi)]
                             (util/>200 ctx (if card-data [card-data] [])))
                           (d/catch clojure.lang.ExceptionInfo
                               (fn [exception-info]
                                 (domain-exception ctx (ex-data  exception-info))))))}}}

   (merge (util/common-resource :me/cards))))

(defn card [user-store mimi]
  (->
   {:methods
    {:get {:parameters {:path {:card-id String}
                        :query {:access_token String}}
           :response (fn [ctx]
                       (-> (d/let-flow [user-id (:_id (util/authenticated-user ctx))
                                        card-data (get-card user-store user-id mimi)]
                             (util/>200 ctx card-data))))}
     :delete {:parameters {:path {:card-id String}
                           :query {:access_token String}}
              :response (fn [ctx]
                          (condp = (get-in ctx [:parameters :query :access_token])
                            "500"    (util/>500 ctx ["Internal Server Error :( " "An unexpected error occurred processing the request."])
                            "403"    (util/>403 ctx ["Forbidden" "You have not been granted permission to access the requested method or object."])
                            "121032" (util/>403 ctx ["Card is reported lost or stolen" ""])
                            "121037" (util/>403 ctx ["Card is closed." ""])
                            "404"    (util/>404 ctx ["Not Found" "Resource was not found"])
                            "121018" (util/>400 ctx ["Cannot unregister a digital card that has a balance greater than zero." "Only zero balance digital cards can be unregistered"])
                            (util/>200 ctx ["OK" "Success"])))}}}

   (merge (util/common-resource :me/cards))))

(defn register-physical [user-store mimi]
  (->
   {:methods
    {:post {:parameters {:query {:access_token String}
                         :body (-> schema :register-physical :post)}
            :response (fn [ctx]
                        (-> (d/let-flow [card-number (-> ctx :parameters :body :cardNumber)
                                         auth-user (util/authenticated-user ctx)
                                         user-id (:_id auth-user)
                                         mimi-res (p/register-physical-card mimi {:cardNumber card-number
                                                                                  :customerId (id>mimi-id user-id)})
                                         card (new-physical-card {:cardNumber card-number})
                                         card-id (insert-card! user-store user-id card)]
                              (util/>200 ctx (merge mocks/card (dummy-card-data) (assoc card :cardId card-id))))

                            (d/catch clojure.lang.ExceptionInfo
                                (fn [exception-info]
                                  (domain-exception ctx (ex-data  exception-info))))))}}}

   (merge (util/common-resource :me/cards))))

(defn register-digital-card [user-store mimi counter-store]
  (->
   {:methods
    {:post {:parameters {:query {:access_token String}}
            :response (fn [ctx]
                        (-> (d/let-flow [card-number (get-next-card-number counter-store)
                                         auth-user (util/authenticated-user ctx)
                                         user-id (:_id auth-user)
                                         mimi-res (p/register-physical-card mimi {:cardNumber card-number
                                                                                  :customerId  (id>mimi-id user-id)})
                                         card (new-digital-card {:cardNumber card-number})
                                         card-id (insert-card! user-store user-id card)]
                              (util/>200 ctx (merge mocks/card (dummy-card-data) (assoc card :cardId card-id))))

                            (d/catch clojure.lang.ExceptionInfo
                                (fn [exception-info]
                                  (domain-exception ctx (ex-data  exception-info))))))}}}

   (merge (util/common-resource :me/cards))))

(def empty-history {:paging {:total 0
                             :offset 0
                             :limit 10
                             :returned 0}
                    :historyItems []})

(defn history [user-store mimi]
  (->
   {:methods
    {:get {:parameters {:query {:access_token String
                                (s/optional-key :limit) String
                                (s/optional-key :offset) String
                                }}
           :response (fn [ctx]
                       (util/>200 ctx empty-history))}}}

   (merge (util/common-resource :me/cards))))

(defn reload [user-store mimi payment-gateway app-config]
  (-> {:methods
       {:post {:parameters {:query {:access_token String}
                            :path {:card-id String}
                            :body (-> schema :reload :post)}
               :response (fn [ctx]
                           (let [card-id (-> ctx :parameters :path :card-id)
                                 amount (-> ctx :parameters :body :amount)]
                             (->
                              (d/let-flow [profile-data (util/user-profile-data ctx user-store (:sub-market app-config))
                                           user-id (:_id (util/authenticated-user ctx))
                                           card-data (:cards (p/find user-store user-id))
                                           card-data-bis (first (filter #(= (:cardId %) card-id) card-data))
                                           _ (log/error ">>>> card-data::::" card-data card-data-bis)
                                           card-number (:cardNumber card-data-bis)
                                           payment-method-data (p/get-payment-method user-store user-id (-> ctx :parameters :body :paymentMethodId))

                                           _ (log/error ">>>> payment-method-data::::" payment-method-data)

                                           payment-data (p/execute-payment
                                                         payment-gateway
                                                         (merge (select-keys profile-data [:emailAddress :lastName :firstName])
                                                                {:amount amount
                                                                 :currency (:currency-code app-config)
                                                                 :cvn "123"
                                                                 :routingNumber (-> payment-method-data :routingNumber)
                                                                 :transactionId "12345"}))
                                           _ (log/info ">>>> payment-data::::" payment-data)
                                           mimi-card-data (p/load-card mimi card-number amount)
                                           _ (log/info "mimi response" mimi-card-data)]
                                          (util/>200 ctx {:balance (:balance mimi-card-data)
                                                          :balanceDate (.toString (java.time.Instant/now))
                                                          :cardId card-id
                                                          :balanceCurrencyCode "ZA"
                                                          :cardNumber card-number}))
                              (d/catch clojure.lang.ExceptionInfo
                                  (fn [exception-info]
                                    (domain-exception ctx (ex-data exception-info)))))))}}}

      (merge (util/common-resource :me/cards))))

(defn autoreload [user-store mimi payment-gateway app-config]
  (-> {:methods
       {:post {:parameters {:query {:access_token String}
                            :path {:card-id String}
                            :body s/Any}
               :response (fn [ctx]
                           (try

                             (util/validate* (-> schema :autoreload :post :paymentMethodId)  (-> ctx :parameters :body :paymentMethodId) [400 "Please supply a payment method id." "Missing payment method identifier attribute is required"])

                             (util/validate* (-> schema :autoreload :post :autoReloadType)  (-> ctx :parameters :body :autoReloadType) [400 "Please supply an auto reload type." "Missing or invalid auto reload type attribute is required. Type must be set to either 'date' or 'amount'."])


                             (util/validate* (-> schema :autoreload :post :day)  (-> ctx :parameters :body :day) [400 "Please supply an auto reload type." "Missing or invalid auto reload type attribute is required. Type must be set to either 'date' or 'amount'."](fn [v]
                                                                                                                                                                                                                                                                               (if (= "Date" (-> ctx :parameters :body :autoReloadType))
                                                                                                                                                                                                                                                                                   (let [v1  (-> ctx :parameters :body :day)]
                                                                                                                                                                                                                                                                                     (and (> v1 0) (< v1 32)))
                                                                                                                                                                                                                                                                                   true)))

                             (util/validate* (-> schema :autoreload :post :amount)  (-> ctx :parameters :body :amount) [400 "Please supply a trigger amount." "For auto reload of type “Amount”, a valid trigger amount attribute is required."])

                             (util/validate* (-> schema :autoreload :post :amount)  (-> ctx :parameters :body :amount) [400 "Please supply an auto reload amount." "Missing or invalid auto reload amount attribute is required. Amount must be within the range of 10-100"] (fn [v] (if (= "Amount" (-> ctx :parameters :body :autoReloadType))
                                                                                                                                                                                                                                                                                 (let [v1  (-> ctx :parameters :body :amount)]
                                                                                                                                                                                                                                                                                   (and (> v1 9) (< v1 101)))
                                                                                                                                                                                                                                                                                 true)))

                             ;; TODO: 400	121033	Invalid operation for card market.
                             ;;       400	121034	Card resource not found to fulfill action.
                             (-> (d/let-flow [auth-user (util/authenticated-user ctx)
                                              payment-method-data (p/get-payment-method user-store (:_id auth-user) (-> ctx :parameters :body :paymentMethodId))

                                              auto-reload-data (p/add-auto-reload user-store (:_id auth-user)
                                                                                  payment-method-data
                                                                                  (-> (select-keys (-> ctx :parameters :body) (keys AutoReloadMongo))
                                                                                      (assoc  :cardId (-> ctx :parameters :path :card-id))))]

                                             (util/>200 ctx {
                                                             :amount (-> ctx :parameters :body :amount)
                                                             :autoReloadId (:autoReloadId auto-reload-data)
                                                             :autoReloadType (-> ctx :parameters :body :autoReloadType)
                                                             :cardId (-> ctx :parameters :path :card-id)
                                                             :day (-> ctx :parameters :body :day)
                                                             :disableUntilDate nil
                                                             :paymentMethodId (-> ctx :parameters :body :paymentMethodId)
                                                             :status (-> ctx :parameters :body :status)
                                                             :stoppedDate nil
                                                             :triggerAmount (-> ctx :parameters :body :triggerAmount)
                                                             }))
                                 (d/catch clojure.lang.ExceptionInfo
                                     (fn [exception-info]
                                       (domain-exception ctx (ex-data exception-info)))))
                             (catch clojure.lang.ExceptionInfo e (domain-exception ctx (ex-data e))))
)}}}

      (merge (util/common-resource :me/cards))))


(defn autoreload-disable [user-store mimi payment-gateway app-config]
  (-> {:methods
       {:put {:parameters {:query {:access_token String}
                            :path {:card-id String}}
               :response (fn [ctx]
                           (-> (d/let-flow [auth-user (util/authenticated-user ctx)
                                            disable-reload-data (p/disable-auto-reload user-store (:_id auth-user))]

                                           (util/>200 ctx [disable-reload-data]))
                               (d/catch clojure.lang.ExceptionInfo
                                   (fn [exception-info]
                                     (domain-exception ctx (ex-data exception-info))))))}}}

      (merge (util/common-resource :me/cards))))

(defn balance [user-store mimi]
  (-> {:methods
       {:get {:parameters {:query {:access_token String}
                           :path {:card-id String}}
              :response (fn [ctx]
                         (d/let-flow [user-id (:_id (util/authenticated-user ctx))
                                      card (get-card user-store user-id mimi)]
                           (util/>200 ctx {:cardId (:cardId card)
                                           :cardNumber (:cardNumber card)
                                           :balance (:balance card)
                                           :balanceDate (.toString (java.time.Instant/now))
                                           :balanceCurrencyCode "ZAR"})))
             }}}
   (merge (util/common-resource :me/cards))))

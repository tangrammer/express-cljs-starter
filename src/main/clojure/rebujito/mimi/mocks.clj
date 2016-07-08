(ns rebujito.mimi.mocks
  (:require [cheshire.core :as json]
            [com.stuartsierra.component :as component]
            [manifold.deferred :as d]
            [taoensso.timbre :as log]
            [rebujito.protocols :as protocols]
            [clojure.string :refer (split)]))

(defn get-balances []
  {:programs
   [{:program "MSR - Customer Tier", :code "MSR001", :balance 50}
    {:program "Starbucks Card", :code "SGC001", :balance 0}
    {:program "MSR - Customer Reward", :code "MSR002", :balance 50}],
   :tier
   {:name "Green",
    :date "2016-07-08",
    :balance 50,
    :pointsUntilNextTier 250},
   :coupons []})

(defn get-history []
  {:transactions
   [{:description "SWARM APPS",
     :amount 0,
     :date "2016-07-08T09:53:25.107Z",
     :check "0",
     :balance 0,
     :id "367779700",
     :items [],
     :location "43362",
     :program "39200"}
    {:description "SWARM APPS",
     :amount 0,
     :date "2016-07-08T09:53:25.187Z",
     :check "0",
     :balance 0,
     :id "367779703",
     :items [],
     :location "43362",
     :program "35622"}
    {:description "SWARM APPS",
     :amount 0,
     :date "2016-07-08T09:53:25.280Z",
     :check "0",
     :balance 0,
     :id "367779706",
     :items [],
     :location "43362",
     :program "41004"}
    {:description "SWARM APPS",
     :amount 0,
     :date "2016-07-08T09:53:26.653Z",
     :check "0",
     :balance 50,
     :id "367779710",
     :items [],
     :location "43362",
     :program "35622"}
    {:description "SWARM APPS",
     :amount 0,
     :date "2016-07-08T09:53:28.477Z",
     :check "0",
     :balance 50,
     :id "367779712",
     :items [],
     :location "43362",
     :program "41004"}
    {:description "SWARM APPS",
     :amount 25,
     :date "2016-07-08T09:59:01.400Z",
     :check "0",
     :balance 25,
     :id "367780003",
     :items [],
     :location "43362",
     :program "39200"}]})

(defrecord MockMimi [base-url token]
  component/Lifecycle
  (start [this]
    this)
  (stop [this] this)
  protocols/Mimi
  (create-account [this data]
    (let [d* (d/deferred)]
      (d/success! d* (-> (str (rand-int 1000000))
                         vector
                         (conj :mock-mimi)))

      d*))
  (remove-account [this data]
    (log/warn "remove-account-mimi! [_ data]" data)
    (clj-bugsnag.core/notify
     (rebujito.MimiException. "TODO: remove-account is not implemented yet!")
          {:api-key (:key (:bugsnag (rebujito.config/config)))
           :environment rebujito.util/*bugsnag-release*
           :meta {:context {:data data}}})
    true)
  (register-physical-card [this data]
    (log/info "(register-physical-card [this data])" " to url: "(format "%s/account/card" base-url))
    (log/debug data)

    (let [d* (d/deferred)]
      (d/future
        (d/success! d* (-> [:success]
                           (conj :prod-mimi))))
      d*))
  (increment-balance! [this card-number amount type]
    (log/debug "(increment-balance! [_ card-number amount type])" card-number amount type)
    {:balance amount})

  (balances [this card-number]
                                        ; TODO: mock the response, don't hit mimi
    (log/info "fetching mock balances for ..." card-number)
    (get-balances)

    )
  (get-history [this card-number]
    (log/info "fetching transactions for" card-number)
    (get-history)
    )
  (transfer [this from to]
    (log/info "transferring card balances from" from "to" to)
    true)
  )

(defn new-mock-mimi [mimi-config]
  (map->MockMimi  mimi-config))

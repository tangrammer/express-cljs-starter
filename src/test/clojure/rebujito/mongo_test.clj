(ns rebujito.mongo-test
  (:require
   [schema-generators.generators :as g]
   [rebujito.config :refer (config)]
   [rebujito.schemas :as rs]
   [manifold.deferred :as d]
   [rebujito.protocols :as p]
   [rebujito.api.resources.card :as card]
   [aleph.http :as http]
   [rebujito.api-test :refer (print-body parse-body create-digital-card*)]
   [rebujito.base-test :refer (system-fixture *system* *user-access-token* get-path)]
   [clojure.test :refer :all]
   [rebujito.mongo :refer (generate-account-id id>mimi-id)]))


(use-fixtures :each (system-fixture #{:+mock-mimi :+ephemeral-db}))



(deftest test-auto-reload-profile
  (testing :test-auto-reload-profile
    (let [user-id (:user-id (p/read-token (:authenticator *system*) *user-access-token*))
          user (p/find (:user-store *system*) user-id)
          card (create-digital-card*)
          card-id (:cardId card)

          ]
      (is (p/add-autoreload-profile-card (:user-store *system*) user-id  (assoc (g/generate rs/AutoReloadMongo)
                                                                            :cardId card-id)))

      (is  (-> (p/find (:user-store *system*) user-id)
               :cards first :autoReloadProfile)))))


(deftest user-store
  (testing :add-auto-reload
    (let [user-id (:user-id (p/read-token (:authenticator *system*) *user-access-token*))
          card (create-digital-card*)
          card-id (:cardId card)
          ]
      (is (nil? (:autoReloadProfile (first (:cards (p/find (:user-store *system*) user-id))))))
      (is (p/add-autoreload-profile-card (:user-store *system*) user-id  (assoc (g/generate rs/AutoReloadMongo)
                                                                      :cardId card-id)))
  ;    (println (p/add-auto-reload (:user-store *system*) user-id {} (g/generate rs/AutoReloadMongo)))
  ;    (println user-id)
      (clojure.pprint/pprint (p/find (:user-store *system*) user-id))
      (is (-> (p/find (:user-store *system*) user-id)
              :cards first :autoReloadProfile))
      (is (-> (p/find (:user-store *system*) user-id)
              :cards first :autoReloadProfile :active))))

  (testing :add-payment-method
    (let [user-id (:user-id (p/read-token (:authenticator *system*) *user-access-token*))]
      (is (nil? (:paymentMethods (p/find (:user-store *system*) user-id))))

      (is (p/add-new-payment-method (:user-store *system*) user-id {:expirationYear 25, :paymentMethodId "X", :default "0o*%Y", :paymentType "", :accountNumberLastFour "'7BG\\T0a}M", :nickName [], :routingNumber "M\\T>vm)=8", :expirationMonth -351}))

      (is (:paymentMethods (p/find (:user-store *system*) user-id)))
      (is (p/add-new-payment-method (:user-store *system*) user-id (g/generate rs/PaymentMethodMongo)))
      (is (:paymentMethods (p/find (:user-store *system*) user-id)))
      ))

  (testing :disable-auto-reload
    (let [user-id (:user-id (p/read-token (:authenticator *system*) *user-access-token*))
          card (-> (p/find (:user-store *system*) user-id)
              :cards first )
          ]
      (is (-> card :autoReloadProfile :active))
      (is (p/disable-auto-reload (:user-store *system*) user-id (:cardId card)) )

      (let [card-number  (:cardNumber (->(p/find (:user-store *system*) user-id) :cards first))
            {:keys [user card]} (p/get-user-and-card (-> *system* :user-store ) card-number)]
        (is card)
        (is (= card-number (:cardNumber card))))


      (is (false? (-> (p/find (:user-store *system*) user-id)
              :cards first :autoReloadProfile :active)))))




  )

(deftest mongo-tests
  (let [api-config (:api (:monks (config :test)))]
    (testing "ApiClient p/login"
      (with-bindings   {#'rebujito.util/*send-bugsnag* false}
        (->
         (p/login (:api-client-store *system*) "XXXX" (:secret api-config))
         (d/chain
          (fn [a]
            (is false)))
         (d/catch clojure.lang.ExceptionInfo (fn [e]

                                               (is (re-find #"invalid_client api client-id" (:body (ex-data e))) ))))



        (->
         (p/login (:api-client-store *system*) (p/generate-id (:api-client-store *system*) "123") (:secret api-config))
         (d/chain
          (fn [a]
            (is false)))
         (d/catch clojure.lang.ExceptionInfo (fn [e]

                                               (is (re-find #"invalid_client api client-id" (:body (ex-data e))) ))))

        (->
         (p/login (:api-client-store *system*) (:key api-config) (:secret api-config))
         (d/chain
          (fn [a]
            (is true)))
         (d/catch clojure.lang.ExceptionInfo (fn [e] (is false)))))))


  (testing "ids"
    (doseq [seed ["42472395" "42485871"]]
      (let [t (generate-account-id seed)]
        (is (= seed (id>mimi-id (str t))))))))




(deftest webhook-store-tests

  (let [uuid "1234567890"
        webhook-uuid (str "webhook/card-number#" uuid)]
    (is (empty? (seq  (p/find (:webhook-store *system*)))))

    (is (=  webhook-uuid  (p/webhook-uuid (:webhook-store *system*) uuid)))
    (is (empty? (seq  (p/find (:webhook-store *system*)))))
    (is (=  {:uuid webhook-uuid
             :state :new}  (select-keys (p/current (:webhook-store *system*) webhook-uuid)
                                        [:uuid :state])))
    (is (= 1 (count(seq  (p/find (:webhook-store *system*))))))
    (is (=  true  (p/change-state (:webhook-store *system*) webhook-uuid :error)))
    (is (=  {:uuid webhook-uuid
             :state "error"}  (select-keys (p/current (:webhook-store *system*) webhook-uuid)
                                           [:uuid :state])))

    (is (=  {:uuid (str webhook-uuid "00")
             :state :new}  (select-keys (p/current (:webhook-store *system*) (str webhook-uuid "00"))
                                        [:uuid :state])))

    (is (= 2 (count(seq  (p/find (:webhook-store *system*))))))
    )



  )

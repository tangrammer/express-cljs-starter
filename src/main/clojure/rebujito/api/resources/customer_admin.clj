(ns rebujito.api.resources.customer-admin
  (:require
   [cheshire.core :as json]
   [taoensso.timbre :as log]
   [manifold.deferred :as d]
   [rebujito.api.resources :refer (domain-exception)]
   [rebujito.api.resources.profile :as profile]
   [rebujito.api.resources.login :as login]
   [rebujito.api.resources.account :as account]
   [rebujito.schemas :refer (UpdateMongoUser)]
   [rebujito.api.resources.card :as card]
   [rebujito.api.resources.addresses :as addresses]
   [rebujito.api.util :as util]
   [rebujito.protocols :as p]
   [rebujito.util :refer (dcatch error*)]
   [schema.core :as s]
   [yada.resource :refer [resource]]
   ))

(def schema {:user {:put UpdateMongoUser}
             :issue-coupon {(s/optional-key :couponType) String
                            :category String
                            :comment String}})

(defn user [user-store mimi]
  (-> {:methods
       {:put    {:parameters {:query {:access_token String}
                              :path {:user-id String}
                              :body (-> schema :user :put)}
                 :response (fn [ctx]
                             (let [try-id ::user
                                   try-type :api]
                               (dcatch ctx
                                       (d/let-flow [payload (util/remove-nils (-> ctx :parameters :body))

                                                    user-id (-> ctx :parameters :path :user-id)
                                                    current-user (p/find user-store user-id)
                                                    email-exists? (when (and (:emailAddress payload) (not= (:emailAddress payload) (:emailAddress current-user)))
                                                                    (account/check-account-mongo {:emailAddress (:emailAddress payload)} user-store))

                                                    user (when-not email-exists?
                                                           (p/find user-store user-id))
                                                    mimi-res (when user
                                                               (p/update-account mimi user))
                                                    updated? (when mimi-res
                                                               (pos? (.getN (p/update-by-id! user-store user-id payload))))

                                                    ]

                                                   (if updated?
                                                     (util/>200 ctx nil)
                                                     (util/>500 ctx "we couldn't update the account"))))))}}
       }
      (merge (util/common-resource :customer-admin))))

(defn forgot-password [user-store mailer authenticator authorizer app-config]
  (-> {:methods
       {:post {:parameters {:query {:access_token String}
                            :path {:user-id String}
                           }
              :response (fn [ctx]
                          (let [try-id ::forgot-password
                                try-type :api]
                            (dcatch ctx
                                    (d/let-flow [user-id (-> ctx :parameters :path :user-id)
                                                 user (p/find user-store user-id)]
                                      (login/forgot-password* ctx user mailer authorizer app-config)))))}}}
      (merge (util/common-resource :customer-admin))))

(defn address [user-store]
  (-> {:methods
       {:put {:parameters {:query {:access_token String}
                           :path {:user-id String
                                  :address-id String}
                           :body (-> addresses/schema :put)
                           }
              :response (fn [ctx]
                          (let [try-id ::address
                                try-type :api]
                            (dcatch ctx
                                   (let [payload (-> ctx :parameters :body)
                                         user-id (-> ctx :parameters :path :user-id)
                                         address-id (-> ctx :parameters :path :address-id)]
                                     (addresses/update-address* ctx payload user-id address-id user-store)))))}}}
      (merge (util/common-resource :customer-admin))))

(defn transfer-to-new-digital [mimi user-store counter-store ]
  (-> {:methods
       {:post {:parameters {:query {:access_token String}
                            :path {:user-id String}}
               :response (fn [ctx]
                           (log/info "transfer-to-new-digital")
                           (let [try-id ::transfer-to
                                 try-type :api]
                             (dcatch ctx
                                     (let [user-id (-> ctx :parameters :path :user-id)
                                           card (card/register-digital-card* counter-store user-store
                                                                             mimi user-id nil)]
                                       (card/transfer_to* ctx
                                                          user-id
                                                          user-store
                                                          mimi
                                                          (:cardNumber card)
                                                          nil)))))}}}
      (merge (util/common-resource :customer-admin))))

(defn transfer-from [mimi user-store]
  (-> {:methods
       {:post {:parameters {:query {:access_token String}
                            :path {:user-id String}
                            :body (-> card/schema :transfer-from :post)}
               :response (fn [ctx]
                           (let [try-id ::transfer-from
                                 try-type :api]
                             (dcatch ctx
                                     (card/transfer-from* ctx
                                                        (-> ctx :parameters :path :user-id)
                                                        user-store
                                                        mimi))))}}}

   (merge (util/common-resource :customer-admin))))

(defn transfer-to [mimi user-store ]
  (-> {:methods
       {:post {:parameters {:query {:access_token String}
                            :path {:user-id String}
                            :body (-> card/schema :transfer-to :post)}
               :response (fn [ctx]
                           (let [try-id ::transfer-to
                                 try-type :api]
                             (dcatch ctx
                                     (card/transfer_to* ctx
                                                        (-> ctx :parameters :path :user-id)
                                                        user-store
                                                        mimi
                                                        (-> ctx :parameters :body :cardNumber)
                                                        (-> ctx :parameters :body :cardPin)))))}}}
      (merge (util/common-resource :customer-admin))))

(defn add-stars [mimi user-store app-config]
  (-> {:methods
       {:put {:parameters {:query {:access_token String}
                           :path {:user-id String}
                           :body {:amount s/Int}
                           }
              :response (fn [ctx]
                          (let [try-id ::add-stars
                                try-type :api]
                            (dcatch ctx
                                    (d/let-flow [user-id (-> ctx :parameters :path :user-id)
                                                 amount (-> ctx :parameters :body :amount)
                                                 user-data (p/find user-store user-id)
                                                 card-number  (let [try-context '[user-id user-data]]
                                                                (or (-> user-data :cards first :cardNumber)
                                                                    #_(error* 500 [500 ::card-number-cant-be-null])))
                                                 res (d/chain
                                                      (p/increment-balance! mimi card-number amount :loyalty)
                                                      (fn [_]
                                                        (p/increment-balance! mimi card-number amount :rewards)))]

                                                (util/>200 ctx (when res nil))))))}}}
      (merge (util/common-resource :customer-admin))))

(defn profile [mimi user-store app-config]
  (-> {:methods
       {:get {:parameters {:query {:access_token String}
                           :path {:user-id String}}
              :response (fn [ctx]
                          (let [try-id ::profile
                                try-type :api]
                            (dcatch ctx
                                    (profile/load-profile ctx mimi user-store (-> ctx :parameters :path :user-id))
                                    )))}}}
      (merge (util/common-resource :customer-admin))))

(defn history [user-store mimi]
  (-> {:methods
       {:get {:parameters {:query {:access_token String}
                           :path {:user-id String}}
              :response (fn [ctx]
                          (dcatch ctx
                                  (card/history* user-store mimi ctx (-> ctx :parameters :path :user-id))))}}}
      (merge (util/common-resource :customer-admin))))

(def SearchSchema
  {:customerId String
   :firstName String
   :lastName String
   :emailAddress String
   :cardNumber String})

(def PagingSchema
  {:total s/Num
   :returned s/Num
   :offset s/Num
   :limit s/Num})

(defn search [mimi user-store app-config]
  (-> {:methods
       {:get {:parameters {:query {:access_token String
                                   :firstname (s/maybe String)
                                   :surname (s/maybe String)
                                   :email (s/maybe String)
                                   :cardnumber (s/maybe String)
                                   (s/optional-key :limit) s/Int
                                   (s/optional-key :offset) s/Int}}
              :response (fn [ctx]
                          (let [try-id ::search
                                try-type :api
                                offset (or (-> ctx :parameters :query :offset) 0)
                                limit (or (-> ctx :parameters :query :limit) 50)]
                            (dcatch ctx
                                    (d/let-flow [[count* users] (let [query (-> ctx :parameters :query)]

                                                                 [(p/search-count user-store
                                                                      (:firstname query)
                                                                      (:surname query)
                                                                      (:email query)
                                                                      (:cardnumber query))

                                                                  (p/search user-store
                                                                             (:firstname query)
                                                                             (:surname query)
                                                                             (:email query)
                                                                             (:cardnumber query)
                                                                             :lastName
                                                                             offset limit)])

                                                 adapt-users (mapv #(let [d %]
                                                                      (merge
                                                                       (select-keys d [:firstName :lastName :emailAddress])
                                                                       (hash-map :customerId (:_id d)
                                                                                 :cardNumber (or (-> d :cards first :cardNumber) ""))))
                                                                   (seq users))]

                                      (util/>200 ctx {:paging {:total count*
                                                               :returned (count adapt-users)
                                                               :offset offset
                                                               :limit limit}
                                                      :customers adapt-users
                                                      })))))}}}
      (merge (util/common-resource :customer-admin))))

(defn issue-coupon [mimi user-store]
  (-> {:methods
       {:post {:parameters {:query {:access_token String}
                            :path {:user-id String}
                            :body (:issue-coupon schema)}
               :response (fn [ctx]
                          (d/let-flow [body (-> ctx :parameters :body)
                                       user-id (-> ctx :parameters :path :user-id)
                                       user-data (p/find user-store user-id)
                                       type "BURN001"
                                       ;  type (or (:couponType body) "BURN001")
                                       comment (:comment body)
                                       category (:category body)
                                       card-number (-> user-data :cards first :cardNumber)
                                       coupon (p/issue-coupon mimi card-number type)]
                            (if coupon
                              (util/>200 ctx {:success true})
                              (util/>500 ctx {:success false})))
                          )}}}
      (merge (util/common-resource :customer-admin))))

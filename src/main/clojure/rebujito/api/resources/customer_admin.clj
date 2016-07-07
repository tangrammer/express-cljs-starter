(ns rebujito.api.resources.customer-admin
  (:require
   [cheshire.core :as json]
   [manifold.deferred :as d]
   [rebujito.api.resources :refer (domain-exception)]
   [rebujito.api.resources.profile :refer (load-profile)]
   [rebujito.api.util :as util]
   [rebujito.protocols :as p]
   [rebujito.util :refer (dcatch error*)]
   [schema.core :as s]
   [yada.resource :refer [resource]]
   ))




(defn profile [mimi user-store app-config]
  (-> {:methods
       {:get {:parameters {:query {:access_token String}
                           :path {:user-id String}}
              :response (fn [ctx]
                          (let [try-id ::profile
                                try-type :api]
                            (dcatch ctx
                                    (load-profile ctx mimi user-store (-> ctx :parameters :path :user-id))
                                    )))}}}
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
                                                      })
))))}}}
      (merge (util/common-resource :customer-admin))))

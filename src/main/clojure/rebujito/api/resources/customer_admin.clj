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
                                   :cardnumber (s/maybe String)}}
              :response (fn [ctx]
                          (let [try-id ::search
                                try-type :api]
                            (dcatch ctx
                                    (d/let-flow [users (let [query (-> ctx :parameters :query)]
                                                         (p/search user-store
                                                                   (:firstname query)
                                                                   (:surname query)
                                                                   (:email query)
                                                                   (:cardnumber query)))
                                                 adapt-users (->> (mapv #(let [d %]
                                                                       (merge
                                                                        (select-keys d [:firstName :lastName :emailAddress])
                                                                        (hash-map :customerId (:_id d)
                                                                                  :cardNumber "")))
                                                                       (seq users))
                                                                  (sort-by :lastName))]

                                      (util/>200 ctx {:paging {:total (count adapt-users)
                                                               :returned (count adapt-users)
                                                               :offset 0
                                                               :limit 0}
                                                      :customers adapt-users
                                                      })
))))}}}
      (merge (util/common-resource :customer-admin))))

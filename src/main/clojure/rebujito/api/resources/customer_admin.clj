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
      (merge (util/common-resource :profile))))

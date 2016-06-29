(ns rebujito.api.resources.social-profile
  (:require
   [rebujito.protocols :as p]
   [rebujito.api.util :refer :all]
   [cheshire.core :as json]
   [manifold.deferred :as d]
   [schema.core :as s]
   [yada.resource :refer [resource]]
   [monger.operators :refer [$set]]))

(def schema {:put {:accountImageUrl String}})

(defn account [user-store]
  (->
   {:methods
    {:put {:parameters {:query {:access_token String}
                        :body (-> schema :put)}
           :response (fn [ctx]
                        (-> (d/let-flow [user-id (:user-id (authenticated-user ctx))
                                         image-url (-> ctx :parameters :body :accountImageUrl)
                                         _ (p/update-by-id! user-store user-id {$set {"socialProfile.account.accountImageUrl" image-url}})]
                              (>200 ctx nil))))}}}

   (merge (common-resource :me/social-profile))))

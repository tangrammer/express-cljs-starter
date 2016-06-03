(ns rebujito.api.resources.rewards
  (:require
   [rebujito.protocols :as p]
   [rebujito.scopes :as scopes]
   [rebujito.api.util :refer :all]
   [rebujito.api.resources :refer (domain-exception)]
   [cheshire.core :as json]
   [manifold.deferred :as d]
   [schema.core :as s]
   [yada.resource :refer [resource]]))

(defn rewards-response [mimi]
  (let [d* (d/deferred)]
    (d/future (d/success! d* (merge rebujito.store.mocks/me-rewards (p/rewards mimi {}))))
    d*
    ))

(defn me-rewards [store mimi user-store authorizer authenticator]
 (resource
  (-> {:methods
       {:get {:parameters {:query {:access_token String
                                   (s/optional-key :select) String
                                   (s/optional-key :ignore) String}}
              :consumes [{:media-type #{"application/json"}
                          :charset "UTF-8"}]
              :response (fn [ctx]
                          (-> (rewards-response mimi)
                              (d/chain
                               (fn [res]
                                 (>200 ctx res)))
                              (d/catch clojure.lang.ExceptionInfo
                                  (fn [exception-info]
                                    (domain-exception ctx (ex-data exception-info))))
                              (d/catch Exception
                                  #(>500* ctx (str "ERROR CAUGHT!" (.getMessage %))))))}}}

      (merge (common-resource :profile))
      (merge (access-control* authenticator authorizer {:get :rebujito.scopes/user})))))

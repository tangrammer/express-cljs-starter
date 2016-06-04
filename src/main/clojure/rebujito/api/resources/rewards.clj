(ns rebujito.api.resources.rewards
  (:require
   [rebujito.protocols :as p]
   [rebujito.scopes :as scopes]
   [rebujito.api.util :refer :all]
   [cheshire.core :as json]
   [schema.core :as s]
   [yada.resource :refer [resource]]))


(defn me-rewards [store mimi user-store authorizer authenticator]
 (resource
  (-> {:methods
       {:get {:parameters {:query {:access_token String
                                   (s/optional-key :select) String
                                   (s/optional-key :ignore) String}}
               :consumes [{:media-type #{"application/json"}
                           :charset "UTF-8"}]
              :response (fn [ctx]
                          (try
                            (>200 ctx (p/rewards mimi {}))
                            (catch Exception e
                              (>500 ctx ["An unexpected error occurred processing the request." (str "caught exception: " (.getMessage e))]))))}}}

      (merge (common-resource :profile))
      (merge (access-control* authenticator authorizer {:get scopes/user})))))

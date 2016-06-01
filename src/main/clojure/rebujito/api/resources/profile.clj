(ns rebujito.api.resources.profile
  (:require
   [rebujito.protocols :as p]
   [rebujito.scopes :as scopes]
   [rebujito.api.util :refer :all]
   [cheshire.core :as json]
   [schema.core :as s]
   [yada.resource :refer [resource]]))



(def schema {:put {:accountImageUrl String}})

(defn me [store mimi user-store authorizer authenticator]
  (resource
   (-> {:methods
        {:get {:parameters {:query {:access_token String
                                    (s/optional-key :select) String
                                    (s/optional-key :ignore) String}}
                :consumes [{:media-type #{"application/json"}
                            :charset "UTF-8"}]
               :response (fn [ctx]
                           (try
                             (let [token (get-in ctx [:parameters :query :access_token])]
                               (if (p/verify authorizer token scopes/application)
                                 (>200 ctx (p/get-profile store))
                                 (>403 ctx ["Unauthorized" "access-token doens't have grants for this resource"])))
                             (catch Exception e
                               (>500 ctx ["An unexpected error occurred processing the request." (str "caught exception: " (.getMessage e))])))

                           )}}}


       (merge (common-resource :profile))
       (merge access-control))))

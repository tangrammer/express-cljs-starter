(ns rebujito.api.resources.profile
  (:require
   [rebujito.protocols :as p]
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
                           (>200 ctx (p/get-profile store))
                           )}}}


       (merge (common-resource :profile))
       (merge access-control))))

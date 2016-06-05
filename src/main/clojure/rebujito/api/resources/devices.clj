(ns rebujito.api.resources.devices
  (:require
   [rebujito.protocols :as p]
   [rebujito.scopes :as scopes]
   [rebujito.api.util :refer :all]
   [cheshire.core :as json]
   [schema.core :as s]
   [yada.resource :refer [resource]]))



(def schema {:post {:id String
                    :deviceType String
                    :osVersion String
                    :uaInboxUserName String
                    :uaDeviceId String
                    :hardwareDeviceId String
                    :applicationId String
                    (s/optional-key s/Keyword) s/Any}})

(defn register [store authorizer authenticator]
 (resource
  (-> {:methods
       {:post {:parameters {:query {:access_token String
                                   (s/optional-key :select) String
                                    (s/optional-key :ignore) String}
                            :body s/Any
                            }
               :consumes [{:media-type #{"application/json"}
                           :charset "UTF-8"}]
               :response (fn [ctx]
                           (>202 ctx nil))}}}

      (merge (common-resource :devices))
      (merge {:access-control {}}))))

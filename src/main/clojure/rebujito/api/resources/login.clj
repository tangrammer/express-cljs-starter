(ns rebujito.api.resources.login
  (:require
   [rebujito.protocols :as p]
   [rebujito.scopes :as scopes]
   [rebujito.api.util :refer :all]
   [cheshire.core :as json]
   [schema.core :as s]
   [yada.resource :refer [resource]]))



(def schema {:put {:accountImageUrl String}})

(defn validate-password [crypto authorizer authenticator ]
  (resource
   (-> {:methods
        {:get {:parameters {:query {:access_token String}
                            :body {(s/optional-key :encoded) Boolean
                                   :password String}}
                :consumes [{:media-type #{"application/json"}
                            :charset "UTF-8"}]
               :response (fn [ctx]

                           (let [token (get-in ctx [:parameters :query :access_token])]
                             (if (p/verify authorizer token scopes/user)
                               (let [user (p/read-token authenticator token)]
                                 (if (= (get-in ctx [:parameters :body :password])
                                        (p/unsign crypto (:password user)))
                                   (>200 ctx "")
                                   (>403 ctx ["Forbidden" "password doesn't match"])))
                               (>403 ctx ["Unauthorized" "access-token doens't have grants for this resource"])))
                           )}}}


       (merge (common-resource :login))
       (merge access-control))))

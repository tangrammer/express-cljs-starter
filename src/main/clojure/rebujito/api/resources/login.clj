(ns rebujito.api.resources.login
  (:require
   [rebujito.protocols :as p]
   [rebujito.scopes :as scopes]
   [rebujito.api.util :refer :all]
   [cheshire.core :as json]
   [schema.core :as s]
   [yada.resource :refer [resource]]))


(def schema {:forgot-password {:post {:userName String
                                      :emailAddress String}}
             :validate-password {:post {(s/optional-key :encoded) Boolean
                                        :password String}}})

(defn forgot-password [authorizer mailer]
  (resource
   (-> {:methods
        {:post {:parameters {:query {:access_token String}
                            :body (-> schema :forgot-password :post)}
                :consumes [{:media-type #{"application/json"}
                            :charset "UTF-8"}]
               :response (fn [ctx]
                           (let [token (get-in ctx [:parameters :query :access_token])]
                             (if (p/verify authorizer token scopes/application)
                               (>200 ctx "")
                               (>403 ctx ["Unauthorized" "access-token doens't have grants for this resource"]))))}}}


       (merge (common-resource :login))
       (merge access-control))))

(defn validate-password [user-store crypto authorizer authenticator ]
  (resource
   (-> {:methods
        {:post {:parameters {:query {:access_token String}
                            :body (-> schema :validate-password :post)}
                :consumes [{:media-type #{"application/json"}
                            :charset "UTF-8"}]
               :response (fn [ctx]

                           (let [token (get-in ctx [:parameters :query :access_token])]
                             (if (p/verify authorizer token scopes/user)
                               (let [user (p/read-token authenticator token)
                                     user (p/find user-store (:_id user))]


                                 (if (p/check crypto (get-in ctx [:parameters :body :password]) (:password user))
                                   (>200 ctx "")
                                   (>403 ctx ["Forbidden" "password doesn't match"])))
                               (>403 ctx ["Unauthorized" "access-token doens't have grants for this resource"])))
                           )}}}


       (merge (common-resource :login))
       (merge access-control))))

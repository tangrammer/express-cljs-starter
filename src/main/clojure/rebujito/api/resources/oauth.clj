(ns rebujito.api.resources.oauth
  (:refer-clojure :exclude [methods])
  (:require
   [rebujito.protocols :as p]
   [rebujito.api.util :refer :all]
   [cheshire.core :as json]
   [schema.core :as s]
   [yada.resource :refer [resource]]))

(def schema {:token-refresh-token {:post (s/schema-with-name {:grant_type String
                                                              :refresh_token String
                                                              :client_id String
                                                              :client_secret String}
                                                             "token-refresh-token")}
             :token-resource-owner {:post (s/schema-with-name {:grant_type String
                                                               :client_id String
                                                               :client_secret String
                                                               :username String
                                                               :password String
                                                               :scope String}
                                                        "token-resource-owner")}})


(defn token-resource-owner [store]
  (resource
   (-> {:methods
        {:post {:parameters {:query {:sig String}
                             :body (s/conditional
                                    #(some? (-> % :refresh_token))
                                    (-> schema :token-refresh-token :post)
                                    #(nil? (-> % :refresh_token))
                                    (-> schema :token-resource-owner :post))}

                :consumes [{:media-type #{"application/json" #_"application/x-www-form-urlencoded"}
                            :charset "UTF-8"}]
                :response (fn [ctx]
                            (condp = (get-in ctx [:parameters :query :sig])
                              "400_1" (>400 ctx ["invalid_request" "The request is missing a required parameter, includes an unsupported parameter value (other than grant type), repeats a parameter, includes multiple credentials, utilizes more than one mechanism for authenticating the client, or is otherwise malformed."])
                              "400_2" (>400 ctx ["invalid_client" "Client authentication failed (e.g. unknown client, no client authentication included, or unsupported authentication method). The authorization server MAY return an HTTP 401 (Unauthorized) status code to indicate which HTTP authentication schemes are supported. If the client attempted to authenticate via the Authorization request header field, the authorization server MUST respond with an HTTP 401 (Unauthorized) status code, and include the WWW-Authenticate response header field matching the authentication scheme used by the client."])
                              "400_3" (>400 ctx ["invalid_grant" "The provided authorization grant (e.g. authorization code, resource owner credentials) or refresh token is invalid, expired, revoked, does not match the redirection URI used in the authorization request, or was issued to another client."])
                              "400_4" (>400 ctx ["unauthorized_client" "The authenticated client is not authorized to use this authorization grant type."])
                              "400_5" (>400 ctx ["unsupported_grant_type" "The authorization grant type is not supported by the authorization server."])
                              "400_6" (>400 ctx ["invalid_scope" "The requested scope is invalid, unknown, malformed, or exceeds the scope granted by the resource owner."])
                              "500" (>500 ctx ["Internal Server Error" "An unexpected error occurred processing the request."])
                              (>200 ctx (when (get-in ctx [:parameters :body :refresh_token])
                                          (p/post-refresh-token store)
                                          (p/post-token-resource-owner store)))))}}}


       (merge (common-resource :oauth))
       (merge access-control))))

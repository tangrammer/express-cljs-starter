(ns rebujito.api.resources.oauth
  (:refer-clojure :exclude [methods])
  (:require
   [taoensso.timbre :as log]
   [manifold.deferred :as d]
   [rebujito.api.resources :refer (domain-exception)]
   [rebujito.protocols :as p]
   [rebujito.scopes :as scopes]
   [rebujito.api.sig :as api-sig]
   [rebujito.api.util :refer :all]
   [cheshire.core :as json]
   [schema.core :as s]
   [yada.resource :refer [resource]]
   [buddy.core.codecs :refer (bytes->hex)]
   ))

(def schema {:token-refresh-token  (s/schema-with-name {:grant_type String
                                                        :refresh_token String
                                                        :client_id String
                                                        :client_secret String
                                                        (s/optional-key :scope) String
                                                        (s/optional-key :timestamp) String}
                                                       "token-refresh-token")
             :token-resource-owner (s/schema-with-name {:grant_type String
                                                        :client_id String
                                                        :client_secret String
                                                        :username String
                                                        :password String
                                                        (s/optional-key :scope) String
                                                        (s/optional-key :timestamp) String}
                                                       "token-resource-owner")
             :token-client-credentials (s/schema-with-name {:grant_type String
                                                            :client_id String
                                                            :client_secret String
                                                            (s/optional-key :scope) String
                                                            (s/optional-key :timestamp) String}
                                                           "token-client-credentials")})

(comment "sb-errors"
         #_(condp = (get-in ctx [:parameters :query :sig])
             "400_1" (>400 ctx ["invalid_request" "The request is missing a required parameter, includes an unsupported parameter value (other than grant type), repeats a parameter, includes multiple credentials, utilizes more than one mechanism for authenticating the client, or is otherwise malformed."])
             "400_2" (>400 ctx ["invalid_client" "Client authentication failed (e.g. unknown client, no client authentication included, or unsupported authentication method). The authorization server MAY return an HTTP 401 (Unauthorized) status code to indicate which HTTP authentication schemes are supported. If the client attempted to authenticate via the Authorization request header field, the authorization server MUST respond with an HTTP 401 (Unauthorized) status code, and include the WWW-Authenticate response header field matching the authentication scheme used by the client."])
             "400_3" (>400 ctx ["invalid_grant" "The provided authorization grant (e.g. authorization code, resource owner credentials) or refresh token is invalid, expired, revoked, does not match the redirection URI used in the authorization request, or was issued to another client."])
             "400_4" (>400 ctx ["unauthorized_client" "The authenticated client is not authorized to use this authorization grant type."])
             "400_5" (>400 ctx ["unsupported_grant_type" "The authorization grant type is not supported by the authorization server."])
             "400_6" (>400 ctx ["invalid_scope" "The requested scope is invalid, unknown, malformed, or exceeds the scope granted by the resource owner."])
             "500" (>500 ctx ["Internal Server Error" "An unexpected error occurred processing the request."])
             ))



(defmulti get-token
  "OAuth Token methods: dispatch on grant_type"
  (fn [ctx store user-store authorizer crypto api-client-store]
    (keyword (-> ctx :parameters :body :grant_type))))

(defmethod get-token :client_credentials ; docs -> http://bit.ly/1sLcJZO
  [ctx store user-store authorizer crypto api-client-store]
  (d/future (>201 ctx (p/grant authorizer {} #{scopes/application}))))

(defmethod get-token :password ; docs -> http://bit.ly/1sLd3YB
  [ctx store user-store authorizer crypto api-client-store]
  (d/let-flow [api-client (p/login api-client-store
                                   (get-in ctx [:parameters :body :client_id])
                                   (get-in ctx [:parameters :body :client_secret]))
               c (api-sig/deferred-check
                   (get-in ctx [:parameters :query :sig])
                   (get-in ctx [:parameters :body :client_id])
                   (get-in ctx [:parameters :body :client_secret]))]
              (if-let [user (-> (p/find user-store {:emailAddress (get-in ctx [:parameters :body :username])
                                                     :password (p/sign crypto (get-in ctx [:parameters :body :password]))})
                                 first
                                 (dissoc :password))]
                (>201 ctx (p/grant authorizer user #{scopes/application scopes/user}))
                (>404 ctx [:user-not-found (get-in ctx [:parameters :body :username])]))))

(defmethod get-token :refresh_token ; docs -> http://bit.ly/1sLcWfw
  ; TODO verify refresh token #39
  ; https://github.com/naartjie/rebujito/issues/39
  [ctx store user-store authorizer crypto api-client-store]
  (d/future (>201 ctx (p/grant authorizer {} #{scopes/application scopes/user}))))

(defn check-value [map key value]
  (let [map (clojure.walk/keywordize-keys map)]
    (= ((keyword key) map) value)))

(defn token-resource-owner [store user-store authorizer crypto api-client-store]
  (resource
   (-> {:methods
        {:post {:parameters {:query {:sig String}
                             :body (s/conditional
                                    #(check-value % :grant_type "client_credentials")
                                    (-> schema :token-client-credentials)
                                    #(check-value % :grant_type "password")
                                    (-> schema :token-resource-owner)
                                    #(check-value % :grant_type "refresh_token")
                                    (-> schema :token-refresh-token))}

                :consumes [{:media-type #{"application/x-www-form-urlencoded"}
                            :charset "UTF-8"}]

                :response (fn [ctx]
                            (-> (get-token ctx store user-store authorizer crypto api-client-store)
                                (d/catch clojure.lang.ExceptionInfo
                                    (fn [exception-info]
                                      (domain-exception ctx (ex-data exception-info))))))}}}

       (merge (common-resource :oauth))
       (merge access-control))))

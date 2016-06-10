(ns rebujito.security.auth
  (:require [buddy.core.mac :as mac]
            [taoensso.timbre :as log]
            [com.stuartsierra.component :refer [system-map system-using using] :as component]
            [rebujito.protocols :as p]
            [plumbing.core :refer [defnk]]
            [buddy.core.codecs :refer (bytes->hex)]))

(defn authentication-from-ctx
  "The ctx argument is the top level yada context.
  This functions returns the authentication map from the default realm
  (the only realm we are using)."
  [ctx]
  (get-in ctx [:authentication "default"]))

(defn user-from-ctx
  "The ctx argument is the top level yada context.
  This functions returns the currently logged-in user from the default realm
  (the only realm we are using)."
  [ctx]
  (get (authentication-from-ctx ctx) :user))

(defrecord OauthAuthorizer [authenticator token-store]
  component/Lifecycle
  (start [this]
    this)
  (stop [this] this)
  p/Authorizer
  (grant [_ data scopes]
    (let [scopes {:scope scopes}
          access-token (p/generate-token authenticator (merge data scopes) 60)
          refresh-token (p/generate-token authenticator (merge data scopes) 1440)
          _ (p/update! token-store {:user-id (:_id data)} {:valid false})
          mongo-token (p/get-and-insert! token-store {:access-token access-token :refresh-token refresh-token :user-id (:_id data) :scopes scopes :valid true})]
      (log/info "MONGO_TOKEN >" mongo-token)
      (merge {:extended nil
              :access_token access-token
              :refresh_token refresh-token
              :return_type "json"
              :state nil
              :token_type "bearer"
              :expires_in 3600
              :uri nil})))

  (verify [this token scope]
    (let [data (p/read-token authenticator token)]
      (contains? (into #{} (:scope data)) (str (.-sym scope))))))

(defn new-authorizer []
  (map->OauthAuthorizer {}))

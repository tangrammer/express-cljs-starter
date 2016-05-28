(ns rebujito.security.auth
  (:require [buddy.core.mac :as mac]
            [com.stuartsierra.component :refer [system-map system-using using] :as component]
            [rebujito.protocols :as p]
            [plumbing.core :refer [defnk]]
            [buddy.core.codecs :refer (bytes->hex)]
            ))

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

(defrecord OauthAuthorizer [authenticator]
  component/Lifecycle
  (start [this]
    this)
  (stop [this] this)
  p/Authorizer
  (grant [_ data scopes]
    (let [scopes {:scope scopes}]
      (merge {:extended nil
              :access_token (p/generate-token authenticator (merge data scopes))
              :refresh_token "todo: "
              :return_type "json"
              :state nil
              :token_type "bearer"
              :expires_in 3600
              :uri nil})))

  (verify [this token scope]
    (let [data (p/read-token authenticator token)]
      (contains? (into #{} (:scope data)) (name scope)))))

(defn new-authorizer []
  (map->OauthAuthorizer {}))

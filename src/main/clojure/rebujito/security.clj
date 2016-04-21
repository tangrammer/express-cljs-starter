(ns rebujito.security
  (:require [buddy.core.codecs :as codecs]
            [rebujito.auth.auth-token :as token]
            [rebujito.protocols :as protocols]
            [com.stuartsierra.component :refer [system-map system-using using] :as component]

            [environ.core :refer [env]]
            [plumbing.core :refer [defnk]]
            [buddy.core.mac :as mac])
  (:import (java.util UUID)))

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

(defrecord Signer [secret-key]
  component/Lifecycle
  (start [this]
    this)
  (stop [this] this)
  protocols/Auth
  (sign [this data expire-in]
    (let [token-id (.toString (UUID/randomUUID))]
     (-> (token/claims data token-id expire-in)
         (token/sign secret-key))))

  (unsign [this jwt]
    (token/unsign jwt secret-key)))

(defn new-security [config]
  (let [{:keys [secret-key]} (:auth config)]
    (->Signer secret-key)))

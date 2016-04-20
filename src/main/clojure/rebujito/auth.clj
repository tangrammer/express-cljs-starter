(ns rebujito.auth
  (:require [buddy.core.codecs :as codecs]
            [rebujito.auth.auth-token :as token]
            [rebujito.protocols :as protocols]
            [com.stuartsierra.component :refer [system-map system-using using] :as component]

            [environ.core :refer [env]]
            [plumbing.core :refer [defnk]]
            [buddy.core.mac :as mac])
  (:import (java.util UUID)))

(defn verify [pass hash secret-key]
  (mac/verify pass (codecs/hex->bytes hash) {:key secret-key :alg :hmac+sha256}))

(defn encrypt [user-pw]
  (-> (mac/hash user-pw {:key (:rebujito-secret-key env)
                         :alg :hmac+sha256})
      (codecs/bytes->hex)))

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

(defn sign*
  "Generates a JWT and signs it."
  ([data secret-key]
   (sign* data secret-key 1440))                                        ; 1 day in minutes
  ([data secret-key expire-in]
   (let [token-id (.toString (UUID/randomUUID))]
     (-> (token/claims data token-id expire-in)
         (token/sign secret-key)))))

(defn unsign*
  "Verifies the signature and claims and returns the "
  [jwt secret-key]
  (token/unsign jwt secret-key))

(defrecord Signer [secret-key]
  component/Lifecycle
  (start [this]
    this)
  (stop [this] this)
  protocols/Auth
  (sign [this data]
    (sign* data secret-key)
    )

  (unsign [this data]
    (unsign* data secret-key))

  )

(defn signer [config]
  (let [{:keys [secret-key]} (:auth config)]
    (->Signer secret-key)))

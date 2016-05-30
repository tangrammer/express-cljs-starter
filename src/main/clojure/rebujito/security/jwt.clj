(ns rebujito.security.jwt
  "Namespace for handling authentication json-web-token creation and validation."
  (:require
   [com.stuartsierra.component :as component]
   [rebujito.protocols :as p]
   [buddy.sign.jws :as jws])
  (:import [java.time ZonedDateTime Duration]
           [java.util UUID]))

(defn claims
  "Returns `data` with added registered JWT claims (http://tools.ietf.org/html/rfc7519#section-4.1).
  Expects `expire-in-minutes` and `valid-in-minutes` to be minutes from now.
  The returned claims are ready to be signed."
  ([data id expire-in-minutes]
   (claims data id expire-in-minutes nil))
  ([data id expire-in-minutes valid-in-minutes]
   (let [now (ZonedDateTime/now)
         valid-in-minutes (or valid-in-minutes 0)
         valid-from (.plusMinutes now valid-in-minutes)
         valid-to (.plusMinutes now expire-in-minutes)]
     (merge data {:iss "rebujito-auth"
                  :iat now
                  :nbf valid-from
                  :exp valid-to
                  :jti id}))))

(defn jws-sign
  "Signs `claims` with `k` and returns the resulting JWT."
  [claims k]
  (jws/sign claims k))

(defn jws-unsign
  "Returns the claims of an signed JWT and throws an exception if one of the registered JWT claims cannot be verified."
  [jwt k]
  (jws/unsign jwt k))

(defrecord JWTAuthenticator [secret-key]
  component/Lifecycle
  (start [this]
    this)
  (stop [this] this)
  p/Authenticator
  (read-token [this token]
    (jws-unsign token secret-key))
  (generate-token [this data]
    (let [expire-in 1440 ;; 1 day in minutes
          token-id (.toString (UUID/randomUUID))]
      (jws-sign (claims data token-id expire-in) secret-key))))

(defn new-authenticator [config]
  (map->JWTAuthenticator config))
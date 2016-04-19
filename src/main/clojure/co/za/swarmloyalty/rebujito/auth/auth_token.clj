(ns co.za.swarmloyalty.rebujito.auth.auth-token
  "Namespace for handling authentication token creation and validation."
  (:require [buddy.sign.jws :as jws])
  (:import (java.time ZonedDateTime)))

(extend-protocol buddy.sign.util/ITimestamp
  ZonedDateTime
  (to-timestamp [obj]
    (.toEpochSecond obj)))

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

(defn sign
  "Signs `claims` with `k` and returns the resulting JWT."
  [claims k]
  (jws/sign claims k))

(defn unsign
  "Returns the claims of an signed JWT and throws an exception if one of the registered JWT claims cannot be verified."
  [jwt k]
  (jws/unsign jwt k))

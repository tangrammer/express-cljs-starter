(ns rebujito.access-token
  "Small ns for access token. Feel free to move this somewhere else if wanted."
  (:require [rebujito.auth.auth-token :as token])
  (:import (java.util UUID)))


(defn access-token [data]
  )

;; TODO@rodrigo move this shit to configuration!
;; And when you already doing this you can get rid of duplicated code here and in auth ;)
(def secret-key "AAAAAAAAAAAAAAAA")


; TODO: remove duplicate code. This only uses a different secrect key (and expiry):)
(defn sign
  "Generates a JWT and signs it."
  ([data]
   (sign data 10080))                                       ; valid for one whole week!! DAAAMN!!
  ([data expire-in]
   (let [token-id (.toString (UUID/randomUUID))]
     (-> (token/claims data token-id expire-in)
         (token/sign secret-key)))))

; TODO: same as above
(defn unsign
  "Verifies the signature and claims and returns the "
  [jwt]
  (token/unsign jwt secret-key))

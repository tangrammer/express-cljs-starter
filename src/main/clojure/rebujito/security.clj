(ns rebujito.security
  (:require [rebujito.mongo :as db]
            [rebujito.protocols :as p]
            [schema.core :as s]
            [taoensso.timbre :as log]
            [rebujito.security.auth :as auth])
  )

(defn extract-data [user]
  (select-keys user [:_id]))

(some? (:a {:a false}))

(defn valid? [protected-data token token-store]
  (log/warn protected-data)

  ;; this only works for token not for refresh-tokens
  #_(s/validate (assoc auth/OauthValidData
                       s/Any s/Any) protected-data)

  (if (some? (:valid protected-data))
    (:valid protected-data)
    (:valid (first (p/find token-store {:refresh-token token})))))

(defn jwt [access-token authenticator expires-in-minutes]
  (let [refresh-token (:refresh-token (p/read-token authenticator access-token))]
    {:extended nil
     :access_token access-token
     :refresh_token refresh-token
     :return_type "json"
     :state nil
     :token_type "bearer"
     :expires_in (* 60 expires-in-minutes)
     :uri nil}))

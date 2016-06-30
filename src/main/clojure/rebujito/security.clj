(ns rebujito.security
  (:require [rebujito.mongo :as db]
            [rebujito.protocols :as p]
            [schema.core :as s]
            [rebujito.security.auth :as auth])
  )

(defn extract-data [user]
  (select-keys user [:_id :firstName :lastName :emailAddress]))

(defn valid? [protected-data]
  (s/validate (assoc auth/OauthValidData
                     s/Any s/Any) protected-data)
    (:valid protected-data))

(defn jwt [access-token authenticator]
  (let [refresh-token (:refresh-token (p/read-token authenticator access-token))]
    {:extended nil
     :access_token access-token
     :refresh_token refresh-token
     :return_type "json"
     :state nil
     :token_type "bearer"
     :expires_in 3600
     :uri nil}))

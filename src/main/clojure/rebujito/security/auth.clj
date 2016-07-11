(ns rebujito.security.auth
  (:require [schema.core :as s]
            [plumbing.core :refer (?>)]
            [buddy.core.mac :as mac]
            [taoensso.timbre :as log]
            [rebujito.util :as util]
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


(defn- verify [authenticator token scope]
    (let [data (p/read-token authenticator token)]
      (contains? (into #{} (:scope data)) (str (.-sym scope)))))


(def OauthGrantedData
  {:scopes {:scope #{s/Keyword}}}
  )

(def OauthUserToken
  {(s/optional-key :firstName) String
   (s/optional-key :lastName) String
   (s/optional-key :emailAddress) String
   (s/optional-key :user-id) org.bson.types.ObjectId
   })

(def OauthValidData {:valid Boolean})

(def OauthData (merge OauthGrantedData
                      OauthUserToken
                      OauthValidData
                      {:refresh-token String}))


(defrecord OauthAuthorizer [authenticator token-store]
  component/Lifecycle
  (start [this]
    this)
  (stop [this] this)
  p/Authorizer
  (grant [this data scopes]
    (p/grant this data scopes 1440))
  (grant [_ data scopes time-in-minutes]
    (let [data (-> data
                   (assoc :refresh-token (p/generate-token authenticator {} time-in-minutes) ;; we only need a random uuid with a time limit to verify origin and expiration time
                          :scope scopes
                          :valid true)
                   (?> (:_id data)
                       (->
                        (assoc :user-id (:_id data))
                        (dissoc :_id))))
;          _ (s/validate OauthData data)

          token-stored (do
                         (when (:user-id data)
                           ;; TODO: filter by scopes!! to not invalidate tokens of diferent types
;                           (p/update! token-store (select-keys data [:user-id]) {:valid false})
                           )
                         (p/get-and-insert! token-store data))

          access-token (p/generate-token authenticator token-stored 60)]
;      (log/info "MONGO_TOKEN >" mongo-token)

      access-token )
    )


  (invalidate! [this token]
    (let [data (p/read-token authenticator token)
          id (:_id data)
          user-id (:user-id data )
          try-type :token-store
          try-id ::invalidate
          try-context '[user-id]]
      (log/info "invalidate token for" user-id)
      (util/dtry
       (do
         (log/warn "invalidating this data " data)
         (p/update-by-id! token-store (:_id data) {:valid false})))))
  (protected-data [this refresh-token]
    (let [refreshable? (p/read-token authenticator refresh-token) ;; checking origin and expiry time
          token-stored (first (p/find token-store {:refresh-token refresh-token}))] ;; checking that exists in db]
      token-stored))

  )


(defn new-authorizer []
  (map->OauthAuthorizer {}))

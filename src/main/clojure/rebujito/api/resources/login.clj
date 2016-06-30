(ns rebujito.api.resources.login
  (:require
   [manifold.deferred :as d]
   [taoensso.timbre :as log]
   [rebujito.protocols :as p]
   [rebujito.scopes :as scopes]
   [rebujito.api.util :as util]
   [rebujito.util :refer (dcatch error*)]
   [cheshire.core :as json]
   [schema.core :as s]
   [yada.resource :refer [resource]]))


(def schema {:reset-username {:post {:password String}}
             :change-username {:put {:new-username String}}
             :change-password {:put {:new-password String}}
             :forgot-password {:post {:userName String
                                      :emailAddress String}}
             :validate-password {:post {(s/optional-key :encoded) Boolean
                                        :password String}}})

(defn- valid-pw? [authenticator user-store crypto token pw]
  (let [user (p/read-token authenticator token)
        user (p/find user-store (:user-id user))
        valid-pw? (p/check crypto pw (:password user))]
    valid-pw?))

(defn change-username [authorizer authenticator user-store ]
  (-> {:methods
       {:put {:parameters {:query {:access_token String}
                            :body (-> schema :change-username :put)}
              :response (fn [ctx]
                           (dcatch ctx
                                   (d/let-flow [user (util/authenticated-user ctx)
                                                user-id (:user-id user)
                                                new-username (get-in ctx [:parameters :body :new-username])
                                                current (p/find user-store user-id)
                                                updated? (p/update-by-id! user-store user-id (merge current {:emailAddress new-username}))]
                                               (if (pos? (.getN updated?))
                                                 (do
                                                   (p/invalidate! authorizer (get-in ctx [:parameters :query :access_token]))
                                                   (util/>200 ctx nil))
                                                 (util/>400 ctx (str "transaction failed"))
                                                 ))))}}}


      (merge (util/common-resource :login))))

(defn reset-username [authorizer authenticator user-store crypto mailer]
  (-> {:methods
       {:post {:parameters {:query {:access_token String}
                            :body (-> schema :reset-username :post)}
              :response (fn [ctx]
                           (dcatch ctx
                                   (if (valid-pw? authenticator user-store crypto
                                                  (get-in ctx [:parameters :query :access_token])
                                                  (get-in ctx [:parameters :body :password]))
                                     (d/let-flow [user (util/authenticated-user ctx)
                                                  data (assoc (select-keys [:emailAddress] user)
                                                              :_id (:user-id user)
                                                              )
                                                  access-token  (p/grant authorizer data #{scopes/reset-username})

                                                  send (p/send mailer {:subject (format "sending OTP to reset email" )
                                                                       :to (:emailAddress user)
                                                                       :content access-token})]
                                                 (if send
                                                   (util/>200 ctx nil)
                                                   (util/>400 ctx send)))
                                     (util/>403 ctx {:message (str "Forbidden: " "password doesn't match")}))))}}}
      (merge (util/common-resource :login))))

(defn change-password [user-store crypto]
  (-> {:methods
       {:put {:parameters {:query {:access_token String}
                            :body (-> schema :change-password :put)}
               :response (fn [ctx]
                           (dcatch ctx
                                   (let [token (get-in ctx [:parameters :query :access_token])]
                                     (do
                                       (d/let-flow [new-password (get-in ctx [:parameters :body :new-password])
                                                    user-id (util/authenticated-user-id ctx)
                                                    updated? (p/update-by-id! user-store user-id {:password (p/sign crypto new-password)})]
                                                   (if (pos? (.getN updated?))
                                                   (util/>200 ctx nil)
                                                   (util/>400 ctx (str "transaction failed"))
                                                   )))
                                     )))}}}


      (merge (util/common-resource :login))))

(defn forgot-password [user-store mailer authenticator]
  (-> {:methods
       {:post {:parameters {:query {:access_token String
                                    (s/optional-key :locale) String}
                            :body (-> schema :forgot-password :post)}
               :response (fn [ctx]
                           (dcatch ctx
                                   (let [token (get-in ctx [:parameters :query :access_token])]
                                     (do
                                       (d/let-flow [params (get-in ctx [:parameters :body])
                                                    user (or (first (p/find user-store {:emailAddress (get-in ctx [:parameters :body :emailAddress])}))
                                                             (error* 400 [400 (format "user %s doesn't exist" (:emailAddress params))]))
                                                    _ (log/info user)
                                                    data (merge (select-keys [:emailAddress :_id] user)
                                                                {:scope #{scopes/reset-password}})
                                                    access-token (p/generate-token authenticator data 60)
                                                    _ (log/info access-token)
                                                    send (p/send mailer {:subject (format "sending forgot-password to %s" (get-in ctx [:parameters :body :userName]))
                                                                         :to (:emailAddress user)

                                                                         :content access-token
                                                                         })
                                                    _ (log/info send)
                                                    ]
                                                   (util/>200 ctx (if send nil send))))
                                     )))}}}


      (merge (util/common-resource :login))))



(defn validate-password [user-store crypto authenticator ]
  (-> {:methods
       {:post {:parameters {:query {:access_token String
                                    (s/optional-key :locale) String}
                            :body (-> schema :validate-password :post)}
               :response (fn [ctx]
                           (dcatch ctx
                                   (if (valid-pw? authenticator user-store crypto
                                                  (get-in ctx [:parameters :query :access_token])
                                                  (get-in ctx [:parameters :body :password]))
                                     (util/>200 ctx nil)
                                     (util/>403 ctx {:message (str "Forbidden: " "password doesn't match")}))))}}}


      (merge (util/common-resource :login))))

(defn logout [authorizer]
 (-> {:methods
      {:get {:parameters {:query {:access_token String
                                  (s/optional-key :locale) String}}
             :response (fn [ctx]
                         (dcatch ctx
                                 (do (p/invalidate! authorizer (-> ctx :parameters :query :access_token))
                                     (util/>200 ctx {:status "ok"}))))}}}

     (merge (util/common-resource :me/login))))

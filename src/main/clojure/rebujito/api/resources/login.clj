(ns rebujito.api.resources.login
  (:require
   [manifold.deferred :as d]
   [taoensso.timbre :as log]
   [rebujito.protocols :as p]
   [rebujito.scopes :as scopes]
   [rebujito.api.util :as util]
   [rebujito.util :refer (dcatch error*)]
   [rebujito.template :as template]
   [cheshire.core :as json]
   [schema.core :as s]
   [yada.resource :refer [resource]]))


(def schema {:reset-username {:post {:password String}}
             :change-email {:post {:new-email String
                                   :password String}}

             :me-change-password {:post {:password String
                                         :new_password String}}

             :set-new-password {:put {:new_password String}}

             :forgot-password {:post {:userName String
                                      :emailAddress String}}
             :validate-password {:post {(s/optional-key :encoded) Boolean
                                        :password String}}})

(defn- valid-pw? [authenticator user-store crypto token pw]
  (let [user (p/read-token authenticator token)
        user (p/find user-store (:user-id user))
        valid-pw? (p/check crypto pw (:password user))]
    (if valid-pw?
      user
      nil)))

(defn change-email [authorizer authenticator user-store ]
  (-> {:methods
       {:put {:parameters {:query {:access_token String}}
              :response (fn [ctx]
                           (dcatch ctx
                                   (d/let-flow [authenticated-data (util/authenticated-data ctx)
                                                user-id (:user-id authenticated-data)
                                                new-email (:new-email authenticated-data)
                                                updated? (p/update-by-id! user-store user-id {:emailAddress new-email
                                                                                              :verifiedEmail true})]
                                               (if (pos? (.getN updated?))
                                                 (do
                                                   (p/invalidate! authorizer (get-in ctx [:parameters :query :access_token]))
                                                   (util/>200 ctx nil))
                                                 (util/>400 ctx (str "transaction failed"))
                                                 ))))}}}


      (merge (util/common-resource :login))))



(defn me-change-email [user-store crypto authenticator authorizer mailer app-config]
  (-> {:methods
       {:post {:parameters {:query {:access_token String}
                            :body (-> schema :change-email :post)}
               :response (fn [ctx]
                           (dcatch ctx
                                   (d/let-flow [valid-data (valid-pw? authenticator user-store crypto
                                                                      (get-in ctx [:parameters :query :access_token])
                                                                      (get-in ctx [:parameters :body :password]))
                                                access-token  (p/grant authorizer
                                                                       (merge (select-keys valid-data [:_id])
                                                                              (select-keys (-> ctx :parameters :body)
                                                                                           [:new-email]))
                                                                       #{scopes/change-email})

                                                send (when (and valid-data access-token)
                                                       (let [link (format "%s/change-email/%s"
                                                                          (:client-url app-config)
                                                                          access-token)]
                                                         (p/send mailer {:subject "Verify your Starbucks Rewards email"
                                                                         :to (-> ctx :parameters :body :new-email)
                                                                         :content-type "text/html"
                                                                         :hidden link
                                                                         :content (template/render-file
                                                                                   "templates/email/verify_email.html"
                                                                                   (merge
                                        ; TODO don't have user yet - removed from template
                                        ; (select-keys user [:firstName :lastName])
                                                                                    {:link link}))
                                                                         })))

                                                ]

                                               (if valid-data
                                                 (if (and access-token send)
                                                   (util/>200 ctx nil)
                                                   (util/>400 ctx (str "transaction failed")))
                                                 (util/>403 ctx {:message (str "Forbidden: " "password doesn't match")})))))}}}


      (merge (util/common-resource :login))))

(defn me-change-password [authorizer authenticator user-store crypto mailer]
  (-> {:methods
       {:post {:parameters {:query {:access_token String}
                            :body (-> schema :me-change-password :post)}
              :response (fn [ctx]
                           (dcatch ctx
                                   (if (valid-pw? authenticator user-store crypto
                                                  (get-in ctx [:parameters :query :access_token])
                                                  (get-in ctx [:parameters :body :password]))
                                     (d/let-flow [authenticated-data (util/authenticated-data ctx)
                                                  user-id (:user-id authenticated-data)
                                                  new-password (get-in ctx [:parameters :body :new_password])
                                                  updated? (p/update-by-id! user-store user-id {:password (p/sign crypto new-password)})]
                                                 (if (pos? (.getN updated?))
                                                   (util/>200 ctx nil)
                                                   (util/>400 ctx (str "transaction failed"))))
                                     (util/>403 ctx {:message (str "Forbidden: " "password doesn't match")}))))}}}
      (merge (util/common-resource :login))))

(defn set-new-password [user-store crypto authorizer]
  (-> {:methods
       {:put {:parameters {:query {:access_token String}
                           :body (-> schema :set-new-password :put)}
               :response (fn [ctx]
                           (dcatch ctx
                                   (let [token (get-in ctx [:parameters :query :access_token])]
                                     (do
                                       (d/let-flow [new-password (get-in ctx [:parameters :body :new_password])
                                                    auth-data (util/authenticated-data ctx)
                                                    user-id (:user-id auth-data)
                                                    updated? (p/update-by-id! user-store user-id {:password (p/sign crypto new-password)})]
                                                   (if (pos? (.getN updated?))
                                                     (do
                                                       (p/invalidate! authorizer auth-data)
                                                       (util/>200 ctx nil))
                                                   (util/>400 ctx (str "transaction failed"))
                                                   )))
                                     )))}}}


      (merge (util/common-resource :login))))


(defn forgot-password* [ctx user mailer authorizer app-config]
  (d/let-flow [user-id (str (:_id user))

               access-token (p/grant authorizer {:_id user-id}  #{scopes/reset-password})

               link (format "%s/reset-password/%s/%s" (:client-url app-config) access-token (:emailAddress user))

               send (p/send mailer {:subject "Reset your Starbucks Rewards Password"
                                    :to (:emailAddress user)
                                    :hidden link
                                    :content-type "text/html"
                                    :content (template/render-file
                                              "templates/email/reset_password.html"
                                              (merge
                                               (select-keys user [:firstName :lastName])
                                               {:link link}))})
               _ (log/info send)
               ]
              (util/>200 ctx (if send nil send))))

(defn forgot-password [user-store mailer authenticator authorizer app-config]
  (-> {:methods
       {:post {:parameters {:query {:access_token String
                                    (s/optional-key :locale) String}
                            :body (-> schema :forgot-password :post)}
               :response (fn [ctx]
                           (dcatch ctx
                                   (let [token (get-in ctx [:parameters :query :access_token])
                                         params (get-in ctx [:parameters :body])

                                         ]
                                     (do
                                       (d/chain
                                        (or (first (p/find user-store {:emailAddress (:emailAddress params)}))
                                                                (error* 400 [400 (format "user %s doesn't exist" (:emailAddress params))]))
                                        (fn [user]
                                          (log/info user)
                                          (forgot-password* ctx user mailer authorizer app-config)))))))}}}


      (merge (util/common-resource :login))))

(defn verify-email [authorizer  user-store ]
  (-> {:methods
       {:put {:parameters {:query {:access_token String}}
              :response (fn [ctx]
                           (dcatch ctx
                                   (d/let-flow [authenticated-data (util/authenticated-data ctx)
                                                user-id (:user-id authenticated-data)
                                                updated? (p/update-by-id! user-store user-id {:verifiedEmail true})]
                                               (if (pos? (.getN updated?))
                                                 (let [res (p/invalidate! authorizer (get-in ctx [:parameters :query :access_token]))]

                                                   (log/info "invalidating!!!"  res)
                                                   (util/>200 ctx nil))
                                                 (util/>400 ctx (str "transaction failed"))
                                                 ))))}}}


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

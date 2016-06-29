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


(def schema {:change-password {:post {:new-password String}}
             :forgot-password {:post {:userName String
                                      :emailAddress String}}
             :validate-password {:post {(s/optional-key :encoded) Boolean
                                        :password String}}})

(comment

  (defn validate-token [authorizer])

  (defn change-password [user-store authorizer])

  )

(defn change-password [user-store crypto]
  (-> {:methods
       {:put {:parameters {:query {:access_token String}
                            :body (-> schema :change-password :post)}
               :response (fn [ctx]
                           (dcatch ctx
                                   (let [token (get-in ctx [:parameters :query :access_token])]
                                     (do
                                       (d/let-flow [new-password (get-in ctx [:parameters :body :new-password])
                                                    user-id (util/authenticated-user-id ctx)
                                                    updated? (p/update-by-id! user-store user-id {:password (p/sign crypto new-password)})]
                                                   (util/>200 ctx updated?)))
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
                                   (let [user (p/read-token authenticator (get-in ctx [:parameters :query :access_token]))
                                         user (p/find user-store (:_id user))]
                                     (if (p/check crypto (get-in ctx [:parameters :body :password]) (:password user))
                                       (util/>200 ctx nil)
                                       (util/>403 ctx {:message (str "Forbidden: " "password doesn't match")})))))}}}


      (merge (util/common-resource :login))))

(defn logout [authorizer]
 (-> {:methods
      {:get {:parameters {:query {:access_token String
                                  (s/optional-key :locale) String}}
             :response (fn [ctx]
                         (dcatch ctx
                                 (do (p/invalidate authorizer (-> ctx :parameters :query :access_token))
                                     (util/>200 ctx {:status "ok"}))))}}}

     (merge (util/common-resource :me/login))))

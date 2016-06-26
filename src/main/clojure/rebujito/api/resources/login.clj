(ns rebujito.api.resources.login
  (:require
   [taoensso.timbre :as log]
   [rebujito.protocols :as p]
   [rebujito.scopes :as scopes]
   [rebujito.api.util :as util]
   [rebujito.util :refer (dcatch)]
   [cheshire.core :as json]
   [schema.core :as s]
   [yada.resource :refer [resource]]))


(def schema {:forgot-password {:post {:userName String
                                      :emailAddress String}}
             :validate-password {:post {(s/optional-key :encoded) Boolean
                                        :password String}}})

(defn forgot-password [mailer authorizer ]
  (-> {:methods
       {:post {:parameters {:query {:access_token String
                                    (s/optional-key :locale) String}
                            :body (-> schema :forgot-password :post)}
               :response (fn [ctx]
                           (dcatch ctx
                                   (let [token (get-in ctx [:parameters :query :access_token])]
                                     (if (p/verify authorizer token scopes/application)
                                       (do
                                         (p/send mailer {:subject (format "sending forgot-password to %s" (get-in ctx [:parameters :body :userName]))
                                                         :to (get-in ctx [:parameters :body :emailAddress])

                                                         :content "TODO: this is the link for your new password! "
                                                         })
                                         (util/>200 ctx nil))
                                       (util/>403 ctx {:message (str "Unauthorized: " "access-token doens't have grants for this resource")})))))}}}


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

(defn logout [user-store token-store]
 (-> {:methods
      {:get {:parameters {:query {:access_token String
                                  (s/optional-key :locale) String}}
             :response (fn [ctx]
                         (dcatch ctx
                                 (do (p/invalidate token-store (util/authenticated-user-id ctx))
                                     (util/>200 ctx {:status "ok"}))))}}}

     (merge (util/common-resource :me/login))))

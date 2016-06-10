(ns rebujito.api.resources.login
  (:require
   [taoensso.timbre :as log]
   [rebujito.protocols :as p]
   [rebujito.scopes :as scopes]
   [rebujito.api.util :as util]
   [cheshire.core :as json]
   [schema.core :as s]
   [yada.resource :refer [resource]]))


(def schema {:forgot-password {:post {:userName String
                                      :emailAddress String}}
             :validate-password {:post {(s/optional-key :encoded) Boolean
                                        :password String}}})

(defn forgot-password [mailer authorizer ]
  (-> {:methods
       {:post {:parameters {:query {:access_token String}
                            :body (-> schema :forgot-password :post)}
               :response (fn [ctx]
                           (let [token (get-in ctx [:parameters :query :access_token])]
                             (if (p/verify authorizer token scopes/application)
                               (do
                                 (p/send mailer {:what "sending forgot-password"
                                                 :data (get-in ctx [:parameters :body])})
                                 (util/>200 ctx ""))
                               (util/>403 ctx ["Unauthorized" "access-token doens't have grants for this resource"]))))}}}


      (merge (util/common-resource :login))))

(defn validate-password [user-store crypto authenticator ]
  (-> {:methods
       {:post {:parameters {:query {:access_token String}
                            :body (-> schema :validate-password :post)}
               :response (fn [ctx]
                           (let [user (p/read-token authenticator (get-in ctx [:parameters :query :access_token]))
                                 user (p/find user-store (:_id user))]
                             (if (p/check crypto (get-in ctx [:parameters :body :password]) (:password user))
                               (util/>200 ctx "")
                               (util/>403 ctx ["Forbidden" "password doesn't match"]))))}}}


      (merge (util/common-resource :login))))

(defn logout [user-store token-store]
 (-> {:methods
      {:get {:parameters {:query {:access_token String}}
             :response (fn [ctx]

                         (p/update! token-store {:user-id (:_id (util/authenticated-user ctx))} {:valid false})
                         (util/>200 ctx {:status "ok"}))}}}

     (merge (util/common-resource :me/login))))

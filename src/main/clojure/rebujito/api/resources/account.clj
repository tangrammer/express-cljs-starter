(ns rebujito.api.resources.account
  (:require
   [manifold.deferred :as d]
   [taoensso.timbre :as log]
   [rebujito.api.util :refer :all]
   [rebujito.mimi :as mim]
   [rebujito.scopes :as scopes]
   [rebujito.protocols :as p]
   [rebujito.api.resources :refer (domain-exception)]
   [buddy.core.codecs :refer (bytes->hex)]
   [schema.core :as s]
   [schema.coerce :as sc]
   [yada.resource :refer [resource]]))

(def schema {:post {
                    :addressLine1 String
                    :birthDay String
                    :birthMonth String
                    :city String
                    :country String
                    :countrySubdivision String
                    :emailAddress String
                    :firstName String
                    :lastName String
                    :password String
                    :postalCode String
                    :receiveStarbucksEmailCommunications String
                    :registrationSource String
                    }})

(def CreateAccountMimiMapping
  {mim/CreateAccountSchema
   (fn [x]
     {
      :birth {:dayOfMonth  (:birthDay x) ;; (:birthDay x) ;; 'YYYY-MM-DD'
              :month       (:birthMonth x)}
      :city (:city x)
      :email (:emailAddress x)
      :firstname (:firstName x)
           ;:gender "male"
      :lastname (:lastName x)
           ;:mobile "String"
           ;:password (:password x)
      :postalcode (:postalCode x)
      :region (:countrySubdivision x)
      })})

(def create-account-coercer (sc/coercer mim/CreateAccountSchema
                                        CreateAccountMimiMapping))

(defmethod domain-exception :mimi [ctx {:keys [status body]}]
  (condp = status
    400 (>400 ctx body)
    500 (>500 ctx body)))

(defn create-account-mongo! [data-account mimi-res user-store crypto]
  (let [mimi-id (first mimi-res)
        mongo-id (p/generate-id user-store mimi-id)
        mongo-account-data (-> data-account
                               (assoc :_id mongo-id)
                               (assoc :password (p/sign crypto (:password data-account)) )
                               )]
    (p/get-and-insert! user-store mongo-account-data)))

(defn create [store mimi user-store crypto]
  (resource
   (-> {:methods
        {:post {:parameters {:query {:access_token String
                                     :market String
                                     (s/optional-key :locale) String}
                             :body (:post schema)}
                :consumes [{:media-type #{"application/json"}
                            :charset "UTF-8"}]
                :response (fn [ctx]
                            (-> (p/create-account mimi (create-account-coercer (get-in ctx [:parameters :body])))
                                (d/chain
                                 (fn [mimi-res]
                                   (let [data-account  (get-in ctx [:parameters :body])
                                         res (create-account-mongo! data-account mimi-res user-store crypto)]
                                     (>201 ctx res))))
                                (d/catch clojure.lang.ExceptionInfo
                                    (fn [exception-info]
                                      (domain-exception ctx (ex-data exception-info))))
                                (d/catch Exception
                                    #(>500* ctx (str "ERROR CAUGHT!" (.getMessage %))))))}}}


       (merge (common-resource :account))
       (merge access-control))))

(defn get-user [store mimi user-store authorizer authenticator]
  (resource
   (-> {:methods
        {:get {:parameters {:query {:access_token String
                                     (s/optional-key :select) String
                                     (s/optional-key :ignore) String}}
                :consumes [{:media-type #{"application/json"}
                            :charset "UTF-8"}]
               :response (fn [ctx]
                           (try
                             (let [token (get-in ctx [:parameters :query :access_token])]
                               (if (p/verify authorizer token scopes/user)
                                 (>201 ctx (p/read-token authenticator token))
                                 (>404 ctx ["Not Found" "Account Profile with given userId was not found."])))
                             (catch Exception e
                               (>500 ctx ["An unexpected error occurred processing the request." (str "caught exception: " (.getMessage e))]))))}}}


       (merge (common-resource :account))
       (merge access-control))))

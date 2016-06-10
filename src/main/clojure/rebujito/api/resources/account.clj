(ns rebujito.api.resources.account
  (:require
   [manifold.deferred :as d]
   [taoensso.timbre :as log]
   [rebujito.api.util :as util]
   [rebujito.mimi :as mim]
   [rebujito.schemas :refer (MongoUser)]
   [rebujito.scopes :as scopes]
   [rebujito.protocols :as p]
   [rebujito.api.resources :refer (domain-exception)]
   [buddy.core.codecs :refer (bytes->hex)]
   [schema.core :as s]
   [schema.coerce :as sc]
   [yada.resource :refer [resource]]))

(def schema {:post {:addressLine1 String
                    :addressLine2 String
                    :birthDay (s/conditional number? Integer :else String)
                    :birthMonth (s/conditional number? Integer :else String)
                    :city String
                    :country String
                    :countrySubdivision String
                    :emailAddress String
                    :firstName String
                    :lastName String
                    :password String
                    :postalCode String
                    :receiveStarbucksEmailCommunications Boolean
                    :registrationSource String
                    (s/optional-key :createDigitalCard) Boolean
                    (s/optional-key :market) String
                    }})

(def CreateAccountMimiMapping
  {mim/CreateAccountSchema
   (fn [x]
     {
      :birth {:dayOfMonth  (str (:birthDay x))
              :month       (str (:birthMonth x))}
      :city (:city x)
      :email (:emailAddress x)
      :firstname (:firstName x)
      :lastname (:lastName x)
      :postalcode (:postalCode x)
      :region (:countrySubdivision x)
      })})

(def create-account-coercer (sc/coercer mim/CreateAccountSchema
                                        CreateAccountMimiMapping))

(defmethod domain-exception :mimi [ctx {:keys [status body]}]
  (condp = status
    400 (util/>400 ctx body)
    500 (util/>500 ctx body)))


(defn create-account-mongo! [data-account mimi-res user-store crypto]
  (let [mimi-id (first mimi-res)
        mongo-id (p/generate-id user-store mimi-id)
        mongo-account-data (-> data-account
                               (assoc :_id mongo-id)
                               (assoc :password (p/sign crypto (:password data-account)) ))]
    (s/validate MongoUser mongo-account-data)
    (p/get-and-insert! user-store mongo-account-data)))

(defn check-account-mongo [data-account user-store]
  (log/info "check-account-mongo" data-account)
  (if (first (p/find user-store data-account))
    (d/error-deferred (ex-info (str "API ERROR!")
                               {:type :api
                                :status 400
                                :body  (format  "Email address %s is not unique" (:emailAddress data-account))}))
    false))

(defn create [store mimi user-store crypto]

  (-> {:methods
       {:post {:parameters {:query {:access_token String
                                    :market String
                                    (s/optional-key :locale) String
                                    (s/optional-key :platform) String}
                            :body (:post schema)}
               :response (fn [ctx]
                           (-> (d/let-flow [mimi-account (d/chain
                                                          (check-account-mongo (select-keys (get-in ctx [:parameters :body]) [:emailAddress]) user-store)
                                                          (fn [b]
                                                            (p/create-account mimi (create-account-coercer (get-in ctx [:parameters :body])))))

                                            mongo-account (create-account-mongo! (get-in ctx [:parameters :body]) mimi-account  user-store crypto)]

                                           (util/>201 ctx (dissoc  mongo-account :password)))
                               (d/catch clojure.lang.ExceptionInfo
                                   (fn [exception-info]
                                     (domain-exception ctx (ex-data exception-info))))))}}}
      (merge (util/common-resource :account))))

(defn me [store mimi user-store app-config]
  (-> {:methods
       {:get {:parameters {:query {:access_token String
                                   (s/optional-key :select) String
                                   (s/optional-key :ignore) String}}
              :response (fn [ctx]
                          (let [auth-user (util/authenticated-user ctx)]
                            (util/>201 ctx (util/generate-user-data auth-user (:sub-market app-config)))))}}}

      (merge (util/common-resource :account))))

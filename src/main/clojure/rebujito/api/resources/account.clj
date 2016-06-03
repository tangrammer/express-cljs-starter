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

;                    (s/optional-key :userName) String
                    :addressLine1 String
                    :addressLine2 String
                    :birthDay (s/conditional number? Long :else String)
                    :birthMonth (s/conditional number? Long :else String)
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
;                    s/Keyword s/Any
                    }})

(def CreateAccountMimiMapping
  {mim/CreateAccountSchema
   (fn [x]
     {
      :birth {:dayOfMonth  (str (:birthDay x)) ;; (:birthDay x) ;; 'YYYY-MM-DD'
              :month       (str (:birthMonth x))}
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

(defn create-account-mongo! [data-account user-store crypto]
  (fn [mimi-res]
    (let [d* (d/deferred)
          mimi-id (first mimi-res)
          mongo-id (p/generate-id user-store mimi-id)
          mongo-account-data (-> data-account
                                 (assoc :_id mongo-id)
                                 (assoc :password (p/sign crypto (:password data-account)) ))

          ]
      (if (first (p/find user-store (select-keys mongo-account-data [:emailAddress]) ))
          (d/error! d* (ex-info (str "API ERROR!")
                                {:type :api
                                 :status 400
                                 :body  "Email address is not unique" }))
          (d/success! d* (p/get-and-insert! user-store mongo-account-data)))
      d*
      )))

(defn create [store mimi user-store crypto authenticator authorizer]
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
                                 (create-account-mongo! (get-in ctx [:parameters :body])  user-store crypto)
                                 (fn [mongo-res]
                                   (>201 ctx (dissoc  mongo-res :password))))
                                (d/catch clojure.lang.ExceptionInfo
                                    (fn [exception-info]
                                      (domain-exception ctx (ex-data exception-info))))
                                (d/catch Exception
                                    #(>500* ctx (str "ERROR CAUGHT!" (.getMessage %))))))}}}
       (merge (common-resource :account))
       (merge (access-control* authenticator authorizer {:post :rebujito.scopes/application})))))

(defn me [store mimi user-store authorizer authenticator]
  (resource
   (-> {:methods
        {:get {:parameters {:query {:access_token String
                                     (s/optional-key :select) String
                                     (s/optional-key :ignore) String}}
               :consumes [{:media-type #{"application/json"}
                            :charset "UTF-8"}]
               :response (fn [ctx]
                           (try
                             (let [res (get-in ctx [:authentication "default"])]
                               (>201 ctx (merge (select-keys res [:firstName :lastName ])
                                                {:subMarket "US"
                                                 :exId "????"
                                                 :partner false})))
                             (catch Exception e
                               (>500 ctx ["An unexpected error occurred processing the request." (str "caught exception: " (.getMessage e))]))))}}}

       (merge (common-resource :account))
       (merge (access-control* authenticator authorizer {:get scopes/user})))))

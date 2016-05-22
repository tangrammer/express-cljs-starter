(ns rebujito.api.resources.account
  (:require
   [manifold.deferred :as d]
   [rebujito.api.util :refer :all]
   [rebujito.mimi :as mim]
   [rebujito.protocols :as p]
   [rebujito.api.resources :refer (domain-exception)]
   [schema.core :as s]
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
      :birth {:dayOfMonth  "01" ;; (:birthDay x) ;; 'YYYY-MM-DD'
              :month       "01"}
      :city (:city x)
      :email (:emailAddress x)
      :firstname "String"
      :gender "String" ;; (male|female)
      :lastname "String"
      :mobile "String"
      :password "String"
      :postalcode "String"
      :region "String"
      })})

(defmethod domain-exception :mimi [ctx {:keys [status body]}]
  (condp = status
    400 (>400 ctx body)
    500 (>500 ctx body)))

(defn- create-account-mongo! [data-account mimi-res user-store]
  (let [mimi-id (first mimi-res)
        mongo-id (p/generate-id user-store mimi-id)
        mongo-account-data (-> data-account
                               (assoc :_id mongo-id))]
    (p/get-and-insert! user-store mongo-account-data)))

(defn create [store mimi user-store]
  (resource
   (-> {:methods
        {:post {:parameters {:query {:access_token String
                                     :market String
                                     (s/optional-key :locale) String}
                             :body (:post schema)}
                :consumes [{:media-type #{"application/json"}
                            :charset "UTF-8"}]
                :response (fn [ctx]
                            (-> (p/create-account mimi (get-in ctx [:parameters :body]))
                                (d/chain
                                 #(let [data-account (get-in ctx [:parameters :body])
                                        res (create-account-mongo! data-account % user-store)]
                                    (>201 ctx res)))
                                (d/catch clojure.lang.ExceptionInfo
                                    #(domain-exception ctx (ex-data  %)))
                                (d/catch Exception  #(do
                                                       (println "exception :::...... " %)
                                                       ;; not deferred (>400 ctx ["No Request supplied" "Create Account Request was malformed." (.getMessage %)])
                                                       (>400* ctx (str "ERROR CATCHED!" (.getMessage %)))))))}}}


       (merge (common-resource :account))
       (merge access-control))))

(defn get-user [store mimi user-store]
  (resource
   (-> {:methods
        {:get {:parameters {:query {:access_token String
                                     (s/optional-key :select) String
                                     (s/optional-key :ignore) String}}
                :consumes [{:media-type #{"application/json"}
                            :charset "UTF-8"}]
               :response (fn [ctx]
                           (try
                             (if-let [res (p/find user-store (get-in ctx [:parameters :query :access_token]))]
                               (>201 ctx res)
                               (>404 ctx ["Not Found" "Account Profile with given userId was not found."]))
                             (catch Exception e
                               (>500 ctx ["An unexpected error occurred processing the request." (str "caught exception: " (.getMessage e))]))))}}}


       (merge (common-resource :account))
       (merge access-control))))

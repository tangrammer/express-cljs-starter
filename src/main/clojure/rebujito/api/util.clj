(ns rebujito.api.util
  (:require [clj-bugsnag.core :as bugsnag]
            [rebujito.config :as config]
            [manifold.deferred :as d]
            [rebujito.schemas :refer (UserProfileData)]
            [rebujito.protocols :as p]
            [schema.core :as s]
            [manifold.stream :as stream]
            [byte-streams :as bs]
            [yada.handler :as yh]
            [taoensso.timbre :as log]))

(defn >base [ctx status body]
;  (log/info "base response >>>> " status body)
  (-> ctx :response (assoc :status status)
      (assoc :body body)))

(defn >400 [ctx body]
  (>base ctx 400 body))

(defn >400* [ctx body]
  (log/error "ERROR >>>> " body)
  (>base ctx 400 body)
;;  (d/error-deferred (ex-info  body {}))
  )

(defn new>500* [ctx {:keys [status body code message] :as ex-data}]
  (log/error "ERROR>>>>>" body)
  (>base ctx 500 ex-data))

(defn >500* [ctx body]
  (log/error "ERROR>>>>>" body)
  (d/error-deferred (ex-info body {})))

(defn >404 [ctx body]
  (>base ctx 404 body))

(defn >403 [ctx body]
  (>base ctx 403 body))

(defn >500 [ctx body]
  (>base ctx 500 body))

(defn >201 [ctx body]
  (>base ctx 201 body))

(defn >202 [ctx body]
  (>base ctx 202 body))

(defn >200 [ctx body]
  (>base ctx 200 body))

(defn log-handler  [ctx]
  (if  (>= (-> ctx :response :status) 400)
    (try
      (let [body (bs/to-string (-> ctx :response :body ))]
       (log/error "CALL >>  "  (:response ctx) " :: " (-> ctx :request :uri) " :::: " (-> ctx :parameters) " :: " body)
       (assoc-in ctx [:response :body] (bs/convert body java.nio.ByteBuffer)))
      (catch Exception e
        (log/error "CALL NO BS!>>  "  (:response ctx) " :: " (-> ctx :request :uri) " :::: " (-> ctx :parameters))
        ))
    (log/info "CALL >>  "  (:response ctx) " :: " (-> ctx :request :uri) " :::: " (-> ctx :parameters))))


(def access-control
  {:access-control {}})

(defn common-resource
  ([desc]
   (let [n (if (keyword? desc)
             (if (namespace desc)
               (clojure.string/join "/" [(namespace desc) (name desc)])
               (name desc))
             desc)]
    (common-resource n n)))
  ([desc swagger-tag]
   (merge {:description desc
           :logger log-handler
           :produces [{:media-type #{"application/json"}
                       :charset "UTF-8"}]
           :swagger/tags (if (vector? swagger-tag)
                           swagger-tag [swagger-tag])}
          access-control)))

(defmethod yada.security/verify :jwt
  [ctx {:keys [verify]}]
  (let [token (get-in ctx [:parameters :query :access_token])]
    (log/debug ">>>>> token" token)
    (verify token)))




;; TODO: returns this kind of result to match starbucks doc
;; (>403 ctx ["Unauthorized" "access-token doens't have grants for this resource"])
;; (>404 ctx ["Not Found" "Account Profile with given userId was not found."])
(defmethod yada.authorization/validate :rebujito [ctx credentials authorization]
  (if-let [methods (:methods authorization)]
    (let [pred (get-in authorization [:methods (:method ctx)])]
      (if (yada.authorization/allowed? pred ctx (set (map keyword (:scope credentials))))
        ctx ; allow
        ;; Reject
        (if credentials
          (d/error-deferred
           (ex-info "Forbidden"
                    {:status 403   ; or 404 to keep the resource hidden
                     ;; But allow WWW-Authenticate header in error
                     :headers (select-keys (-> ctx :response :headers) ["www-authenticate"])}))
          (d/error-deferred
           (ex-info "No authorization provided"
                    {:status 401   ; or 404 to keep the resource hidden
                     ;; But allow WWW-Authenticate header in error
                     :headers (select-keys (-> ctx :response :headers) ["www-authenticate"])})))))
    ctx ; no method restrictions in place
    ))


(defn access-control* [authenticator authorizer authorization]
  ;; authorization is something like
  ;;  {:get    :admin
  ;;   :post   :admin
  ;;   :put    :admin
  ;;   :delete :admin}

  {:access-control (merge {:scheme :jwt
                           :verify (fn [token]
                                     (p/read-token authenticator token))}
                          {:authorization {:scheme :rebujito
                                           :methods  authorization}})})

(defn authenticated-data [ctx]
  (get-in ctx [:authentication "default"]))

(defn authenticated-user-id [ctx]
  (:user-id (authenticated-data ctx)))


(defn generate-user-data [readed-jwt sub-market]
  (merge (s/validate UserProfileData (select-keys readed-jwt [:firstName :lastName :emailAddress]))
         {:subMarket sub-market
          :exId nil
          :partner false}))


(defn user-profile-data [ctx user-store submarket]
  (let [auth-user-id (authenticated-user-id ctx)
        mongo-user (p/find user-store (:user-id auth-user-id))]
    (generate-user-data mongo-user submarket)))


(defn field? [m kw]
  (when  (-> m kw)
    {kw (-> m kw)})
  )


(defn read-string* [s]
  (if (= "" s)
    0
    (read-string s)))

(defn validate*
  ([t v [status message description]]
   (validate* t v [status message description] (constantly true)))
  ([t v [status message description] v-fn]
   (try
     (s/validate t v)
     (when-not (v-fn v)
       (throw (ex-info message {:type :schema
                                :status status
                                :code 12345
                                :message (str message " :: " description)
                                :body (str message " :: " description)})))
     (catch Exception e (throw (ex-info message {:type :schema
                                                 :status status
                                                 :code 12345
                                                 :message (str message " :: " description)

                                                 :body (str message " :: " description)}))))))

(def optional-risk {(s/optional-key :risk) {(s/optional-key :platform) String
                                            (s/optional-key :reputation) {(s/optional-key :IPAddress) String
                                                                          (s/optional-key :deviceFingerprint ) String}}})

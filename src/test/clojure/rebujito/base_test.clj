(ns rebujito.base-test
  (:require
   [byte-streams :as bs]
   [cheshire.core :as json]
   [bidi.bidi :as bidi]
   [com.stuartsierra.component :as component]
               [rebujito.system.dev-system :as dev]
               [rebujito.api.sig :as api-sig]
               [rebujito.api.time :as api-time]
               [aleph.http :as http]
               [rebujito.api.resources
                [oauth :as oauth]
                [account :as account]]
               [rebujito.config :refer (config)]
               [schema.core :as s]
               [schema-generators.generators :as g]
               [buddy.core.nonce :as nonce]
               [buddy.core.codecs :refer (bytes->hex)]
               [clojure.test :refer (is)]
               ))

(def ^:dynamic *system* nil)
(def ^:dynamic *user-access-token* nil)
(def ^:dynamic *app-access-token* nil)
(def ^:dynamic *user-account-data* nil)

(defn generate-random [n]
  (-> (nonce/random-bytes n)
      (bytes->hex)
      )
  )

(defn new-account-sb []
  {:countrySubdivision "aa",
   :registrationSource "aa",
   :addressLine1 "zz",
   :addressLine2 "yy",
   :password "real-secret",
   :emailAddress (format  "%s@hola.com" (generate-random 6)),
   :city "Sevilla",
   :firstName (format  "Juan-%s" (generate-random 6))
   :birthDay "13",
   :birthMonth "06",
   :lastName (format  "Ruz-%s" (generate-random 6))
   :receiveStarbucksEmailCommunications true,
   :postalCode "41003",
   :country "Spain"})

(defn api-config []
  (-> (config :test) :api))

(defn new-sig []
  (let [{:keys [key secret]} (api-config)
        t (api-time/now)]
;;    (println ">>>>" (to-timestamp t))
    (api-sig/new-sig t key secret)))

(defn get-path
  ([kw]
   (get-path *system* kw))
  ([s kw ]
   (let [r (-> s :docsite-router :routes)
         sig (new-sig)
         api-id kw]
     (bidi/path-for r api-id))))

(defn access-token-application
  ([]
   (access-token-application *system*))
  ([s]
   (let [r (-> s :docsite-router :routes)
         port (-> s  :webserver :port)
         new-account (g/generate (:post account/schema))
         new-account (new-account-sb)
         sig (new-sig)
         access_token (atom "")]

     (let [api-id ::oauth/token-resource-owner
           path (bidi/path-for r api-id)]
                                        ;     (println r api-id path)
       ;; :grant_type ""client_credentials""
       (is (= 201 (-> (let [r @(http/post (format "http://localhost:%s%s?sig=%s"  port path sig)
                                          {:throw-exceptions false
                                           :form-params (assoc (g/generate (-> oauth/schema :token-client-credentials))
                                                               :grant_type "client_credentials"
                                                               :client_id (:key (api-config))
                                                               :client_secret (:secret (api-config)))
                                           :body-encoding "UTF-8"
                                           :content-type :application/x-www-form-urlencoded})
                            body (-> r :body bs/to-string (json/parse-string true))
                                        ;                          _ (println body)
                            ]
                        (reset! access_token (:access_token body))
                        r)
                      :status)))
       )
     @access_token)))

(defn access-token-user
  ([username password]
   (access-token-user *system* username password))
  ([s username password]
   (let [r (-> s :docsite-router :routes)
         port (-> s  :webserver :port)
         sig (new-sig)
         access_token (atom "")]

     (let [api-id ::oauth/token-resource-owner
           path (bidi/path-for r api-id)]

       (-> (let [r @(http/post (format "http://localhost:%s%s?sig=%s"  port path sig)
                               {:throw-exceptions false
                                :form-params
                                (assoc (g/generate (-> oauth/schema :token-resource-owner))
                                       :grant_type "password"
                                       :client_id (:key (api-config))
                                       :client_secret (:secret (api-config))
                                       :username username
                                       :password password
                                       )
                                :body-encoding "UTF-8"
                                :content-type :x-www-form-urlencoded})
                 body (-> r :body bs/to-string (json/parse-string true))
                 ;;                  _ (println body)

                 ]
             (reset! access_token (:access_token body))
             ;;            (println "\n >>>> password access_token "@access_token "\n")
             r)
           :status)

       )
     @access_token)))


(defn create-account
  ([account-data]
   (create-account *system* account-data))
  ([s account-data]
   (let [port (-> s  :webserver :port)
         path (get-path s ::account/create)
         access_token (access-token-application s)]
     (let [res @(http/post (format "http://localhost:%s%s?access_token=%s&market=%s"  port path access_token 1234)
                           {:throw-exceptions false
                            :body  (json/generate-string account-data)
                            :body-encoding "UTF-8"
                            :content-type :json})]
       res
       (is (= 201 (:status res) ))
       (json/parse-string (bs/to-string (:body res)) true)
       )
     ))
  )
(defn bind-new-user-and-token [s account-data] (let [account (create-account s account-data)
                                                     port (-> s  :webserver :port)
                                                     path (get-path s ::account/me)]
                                   (access-token-user s (:emailAddress account-data)(:password account-data))))
(defmacro with-system [system & body]
  `(let [s# (component/start ~system)
         u# (assoc (new-account-sb)
                   :birthDay "1"
                   :birthMonth "1")]
     (try
       (binding [*system* s#
                 *user-account-data* u#
                 *app-access-token* (access-token-application s#)
                 *user-access-token* (bind-new-user-and-token s# u#)] ~@body)
       (finally
         (component/stop s#)))))

(defn system-fixture [config-env]
  (fn[f]
    (with-system (-> (dev/new-dev-system config-env (update-in (config :test) [:yada :port]
                                                               (comp inc (fn [s]
                                                                           (if (= String (type s))
                                                                             (read-string s)
                                                                             s) )))))
      (try
        (s/with-fn-validation
          (f))
        (catch Exception e (do (println (str "caught exception: " (.getMessage e)))
                               (throw e)))))))

(ns rebujito.base-test
  (:require
   [byte-streams :as bs]
   [cheshire.core :as json]
   [clojure.repl :refer (pst)]
   [bidi.bidi :as bidi]
   [rebujito.logging :as log-levels]
   [rebujito.schemas :refer (MongoUser MimiUser)]
   [rebujito.protocols :as p]
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
(def ^:dynamic *customer-admin-data* nil)
(def ^:dynamic *customer-admin-access-token* nil)

(defn generate-random [n]
  (-> (nonce/random-bytes n)
      (bytes->hex)
      )
  )

(defn generate-mail [format-pattern]
  (format format-pattern (generate-random 6)))

(defn new-account-sb []
  {
   :countrySubdivision "aa",
   :registrationSource "aa",
   :addressLine1 "zz",
   :addressLine2 "yy",
   :password "real-secret",
   :emailAddress (generate-mail "juanantonioruz+%s@gmail.com")
   :city "Sevilla",
   :firstName (format  "Juan-%s" (generate-random 6))
   :birthDay "13",
   :market "ZA"
   :birthMonth "06",
   :lastName (format  "Ruz-%s" (generate-random 6))
   :receiveStarbucksEmailCommunications true,
   :postalCode "41003",
   :country "Spain"})

(s/validate MongoUser (new-account-sb))

(defn api-config []
  (-> (config :test) :monks :api))

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
                                                               :client_secret (:secret (api-config))
                                                               :risk {})

                                           :body-encoding "UTF-8"
                                           :content-type :application/x-www-form-urlencoded})
                            body (-> r :body bs/to-string (json/parse-string true))
                                        ;                          _ (println body)
                            ]
;                        (clojure.pprint/pprint body)
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
                                       :risk {}
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
       (println "test account _________________________")
       (clojure.pprint/pprint (json/parse-string (bs/to-string (:body res)) true))
       )
     ))
  )
(defn insert-new-api-key
  ([system who id secret]
   (p/get-and-insert! (-> system :api-client-store) {:secret secret :who who :_id (rebujito.mongo/to-mongo-object-id id) })))

(defn check-monks-api-key [system]
  (try
    (let [u {:who "media-monks"}
          api-config (:api (:monks (config :test)))]
     (if-let [res (first (p/find (-> system :api-client-store) u))]
       (println (str "api-key"  #_res))
       (do
         (println (str "creating api-key media-monks"))
         (insert-new-api-key system :media-monks (:key api-config) (:secret api-config))
         )))
    (catch Exception e (do
                         (println (type  e) (.getMessage e))
                         (.printStackTrace e)
                         (pst e)
                         (check-monks-api-key system)))))

(defn bind-new-user-and-token [s account-data] (let [account (create-account s account-data)
                                                     port (-> s  :webserver :port)
                                                     path (get-path s ::account/me)]
                                   (access-token-user s (:emailAddress account-data)(:password account-data))))
(defmacro with-system [system & body]
  `(let [s# (component/start ~system)
         u# (assoc (new-account-sb)
                   :birthDay "1"
                   :birthMonth "1")
         customer-admin-data# (assoc (new-account-sb)
                                     :emailAddress (-> (config :test) :app-config :customer-admin)
                                     :birthDay "1"
                                     :birthMonth "1")
         ]
     (try
       (check-monks-api-key s#)
       (binding [*system* s#
                 *user-account-data* u#
                 *customer-admin-data* customer-admin-data#
                 *app-access-token* (access-token-application s#)
                 *user-access-token* (bind-new-user-and-token s# u#)
                 *customer-admin-access-token* (bind-new-user-and-token s# customer-admin-data#)

                 ] ~@body)
       (finally
         (component/stop s#)))))

(defn system-fixture [config-env]
  (fn[f]
    (alter-var-root (var rebujito.util/*send-bugsnag*) (fn [d] true))
    (alter-var-root (var rebujito.util/*bugsnag-release*) (fn [d] "test"))
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



(defn log-config [config-data]
  (-> log-levels/timbre-info-config
      (assoc  :level :debug
              :output-fn log-levels/default-output-fn
              :middleware [(fn [{:keys [level vargs ?ns-str ] :as data}]
                             (let [should-log? (log-levels/log? [?ns-str  level] config-data)]
                                        ;                                                (println ?ns-str level should-log?)
                               (when should-log?
                                 data)))])

      (update-in [:ns-blacklist]
                 (fn [c]
                   (conj c
                         "org.mongodb.driver.cluster"
                         "org.mongodb.driver.connection"
                         "org.mongodb.driver.protocol.*"
                         "io.netty.buffer.PoolThreadCache"))) ))

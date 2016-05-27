(ns rebujito.api-test
  (:require
   [buddy.sign.util :refer (to-timestamp)]
   [rebujito.api.sig :as api-sig]
    [rebujito.api.time :as api-time]

   [aleph.http :as http]
   [org.httpkit.client :as http-k]
   [clj-http.client :as http-c]
   [bidi.bidi :as bidi]
   [byte-streams :as bs]
   [cheshire.core :as json]
   [clojure.pprint :refer (pprint)]
   [clojure.test :refer :all]
   [com.stuartsierra.component :as component]
   [rebujito.api.resources.account :as account]
   [rebujito.api.resources.card :as card]
   [rebujito.api.resources.oauth :as oauth]
   [rebujito.store.mocks :as mocks]
   [rebujito.api.resources.payment :as payment]
   [rebujito.api.resources.social-profile :as social-profile]
   [rebujito.config :refer (config)]
   [rebujito.system :refer (new-production-system)]
   [schema-generators.generators :as g]
   [schema.core :as s]
   [manifold.deferred :as d]
   [rebujito.system.dev-system :as dev]
   ))

(def ^:dynamic *system* nil)

(defmacro with-system [system & body]
  `(let [s# (component/start ~system)]
     (try
       (binding [*system* s#] ~@body)
       (finally
         (component/stop s#)))))

(defn system-fixture [config-env]
  (fn[f]
    (with-system (-> (dev/new-dev-system config-env (update-in (config :test) [:yada :port] inc)))
      (try
        (s/with-fn-validation
          (f))
        (catch Exception e (do (println (str "caught exception: " (.getMessage e)))
                               (throw e)))))))

(use-fixtures :each (system-fixture #{:+mock-mimi}))

(defn api-config []
  (-> (config :test) :api))

(defn new-sig []
  (let [{:keys [key secret]} (api-config)
        t (api-time/now)]
    (println ">>>>" (to-timestamp t))
    (api-sig/new-sig t key secret)))

(defn print-body [c]
  (println ">>>>> ****"(-> c :body bs/to-string))
  c
  )

(deftest test-20*
  (time
   (let [r (-> *system* :docsite-router :routes)
         port (-> *system*  :webserver :port)
         new-account (g/generate (:post account/schema))
         sig (new-sig)]
     (testing ::account/create
       (let [api-id ::account/create
             path (bidi/path-for r api-id)]
         (println (format "http://localhost:%s%s?access_token=%s&market=%s"  port path 123 1234))
         (is (= 201  (-> @(http/post (format "http://localhost:%s%s?access_token=%s&market=%s"  port path 123 1234)
                                        {:throw-exceptions false
                                         :body (json/generate-string
                                                (assoc new-account
                                                       :birthDay "1"
                                                       :birthMonth "1"
                                                       ))
                                         :body-encoding "UTF-8"
                                         :content-type :json})
                         #_print-body
                            :status)))))

     (testing ::oauth/token-resource-owner
       (let [api-id ::oauth/token-resource-owner
             path (bidi/path-for r api-id)]

         ;; body conform :token-refresh-token schema
         #_(is (= 200 (-> @(http/post (format "http://localhost:%s%s?sig=%s"  port path sig)
                                    {:throw-exceptions false
                                     :body (json/generate-string
                                            (g/generate (-> oauth/schema :token-refresh-token :post)))
                                     :body-encoding "UTF-8"
                                     :content-type :json})
                        :status)))
         ;; body conform :token-resource-owner schema
         (is (= 201 (-> @(http/post (format "http://localhost:%s%s?sig=%s"  port path sig)
                                    {:throw-exceptions false
                                     :body (json/generate-string
                                            (assoc (g/generate (-> oauth/schema :token-resource-owner :post))
                                                   :client_id (:key (api-config))
                                                   :client_secret (:secret (api-config))
                                                   :username (:emailAddress new-account)
                                                   :password (:password new-account)
                                                   ))
                                     :body-encoding "UTF-8"
                                     :content-type :json})
      ;;                  print-body
                        :status)))))

     (testing ::card/get-cards
       (let [api-id ::card/get-cards
             path (bidi/path-for r api-id)]
         (is (= 201 (-> @(http/get (format "http://localhost:%s%s?access_token=%s"  port path 123)
                                   {:throw-exceptions false
                                    :body-encoding "UTF-8"
                                    :content-type :json})
                        :status)))))

     (testing ::card/register-physical
       (let [api-id ::card/register-physical
             path (bidi/path-for r api-id)]
         (println (format "http://localhost:%s%s?access_token=%s"  port path 123))
         (is (= 200 (-> @(http/post (format "http://localhost:%s%s?access_token=%s"  port path 123)
                                    {:throw-exceptions false
                                     :body-encoding "UTF-8"
                                     :body (json/generate-string
                                            (g/generate (-> card/schema :post :register-physical)))
                                     :content-type :json})
    ;                    print-body
                        :status)))))

     (testing ::card/register-digital-cards
       (let [api-id ::card/register-digital-cards
             path (bidi/path-for r api-id)]
         (println (format "http://localhost:%s%s?access_token=%s"  port path 123))
         (is (= 201 (-> @(http/post (format "http://localhost:%s%s?access_token=%s"  port path 123)
                                    {:throw-exceptions false
                                     :body-encoding "UTF-8"
                                     :content-type :json})
                        :status)))))

     (testing ::card/unregister
       (let [api-id ::card/unregister
             path (bidi/path-for r api-id :card-id 123)]
         (is (= 200 (-> @(http/delete (format "http://localhost:%s%s?access_token=%s"  port path 123)
                                      {:throw-exceptions false
                                       :body-encoding "UTF-8"
                                       :content-type :json})
                        :status)))))

     (testing ::payment/methods
       (let [api-id ::payment/methods
             path (bidi/path-for r api-id)]
         (is (= 200 (-> @(http/get (format "http://localhost:%s%s?access_token=%s"  port path 123)
                                   {:throw-exceptions false
                                    :body-encoding "UTF-8"
                                    :content-type :json})
                        :status)))
         (is (= 201 (-> @(http/post (format "http://localhost:%s%s?access_token=%s"  port path 123)
                                   {:throw-exceptions false
                                    :body-encoding "UTF-8"
                                    :body (json/generate-string
                                           (g/generate (-> payment/schema :methods :post)))
                                    :content-type :json})
                        :status)))))

     (testing ::social-profile/account
       (let [api-id ::social-profile/account
             path (bidi/path-for r api-id)]
         (is (= 200 (-> @(http/put (format "http://localhost:%s%s?access_token=%s"  port path 123)
                                   {:throw-exceptions false
                                    :body-encoding "UTF-8"
                                    :body (json/generate-string
                                           (g/generate (-> social-profile/schema :put)))
                                    :content-type :json})
                        :status))))))))

(ns rebujito.example-test
  (:require [aleph.http :as http]
            [schema.core :as s]
            [byte-streams :as bs]
            [rebujito.system :refer (new-production-system)]
            [rebujito.config :refer (config)]
            [clojure.pprint :refer (pprint)]
            [com.stuartsierra.component :as component]
            [rebujito.api.resources.account :as account]
            [clojure.test :refer :all]
            [cheshire.core :as json]
            [bidi.bidi :as bidi]
            [schema-generators.generators :as g]
            ))

(def ^:dynamic *system* nil)

(defmacro with-system [system & body]
  `(let [s# (component/start ~system)]
     (try
       (binding [*system* s#] ~@body)
       (finally
         (component/stop s#)))))

(defn system-fixture [f]
  (with-system (-> (new-production-system (update-in (config :test) [:yada :port] inc)))
    (try
      (s/with-fn-validation
        (f))
      (catch Exception e (do (println (str "caught exception: " (.getMessage e)))
                             (throw e))))))

(use-fixtures :each system-fixture)

(deftest test-20*
  (let [r (-> *system* :docsite-router :routes)]

    (testing ::account/create
      (let [api-id ::account/create
            path (bidi/path-for r api-id)]
        (is (= 201 (-> @(http/post (format "http://localhost:%s%s?access_token=%s&market=%s"  (-> *system*  :webserver :port) path 123 1234)
                                   {:throw-exceptions false
                                    :body (json/generate-string
                                           (g/generate (:post account/schema)))
                                    :body-encoding "UTF-8"
                                    :content-type :json})
                       :status)))))))

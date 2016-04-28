(ns rebujito.example-test
  (:require [aleph.http :as http]
            [schema.core :as s]
            [byte-streams :as bs]
            [rebujito.system :refer (new-production-system)]
            [rebujito.config :refer (config)]
            [com.stuartsierra.component :as component]
            [clojure.test :refer :all]
            ))

(def ^:dynamic *system* nil)

(defmacro with-system [system & body]
  `(let [s# (component/start ~system)]
     (try
       (binding [*system* s#] ~@body)
       (finally
         (component/stop s#)))))

#_(-> @(http/get "https://google.com/")
  :body
  bs/to-string
  prn)

(defn system-fixture [f]
  (with-system (-> (new-production-system
                    {:yada {:port 3001}}))
    (s/with-fn-validation
      (f))))

(comment
 (def d (component/start (new-production-system
                          (update-in (config :test) [:yada :port] inc))))

 (:docsite-router :security :api :yada :webserver :jquery :swagger-ui)

  (:handler :port :raw-stream? :request-handler :server)
  (-> d :webserver :port)
  (-> d :webserver :request-handler :routes)
 (component/stop d))



(use-fixtures :each system-fixture)




(deftest test-welcome
  (testing "hola"
    (println (keys *system*)
             (:yada *system*))
    (is (= 1 1))))

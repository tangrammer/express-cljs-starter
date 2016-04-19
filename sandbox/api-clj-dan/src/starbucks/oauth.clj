(ns starbucks.oauth
  (:require
   [clojure.tools.logging :as log]
   [clojure.java.io :as io]
   [hiccup.core :refer [html]]
   [schema.core :as s]
   [cheshire.core :as json]
   [buddy.core.hash :as hash]
   [buddy.core.codecs :as cod]
   [clj-time.coerce :as tcc]
   [clj-time.core :as tcor]
   [starbucks.db :as db]
   [starbucks.schema :as sch]
   [starbucks.html :as html]
   [clojure.test :refer :all]
   ))


(defn make-sig
  [apikey secret timestamp]
  (-> (str apikey secret timestamp)
      hash/md5
      cod/bytes->hex))

(deftest make-sig-test
  (is (= "65a08176826fa4621116997e1dd775fa"
         (make-sig  "2fvmer3qbk7f3jnqneg58bu2"
                    "qvxkmw57pec7"
                    1200603038))))



(defn valid-sig?
  "Validate signature.
  params: sig - signature (string)
          apikey - x api key (string)
          secret - user's share secret (string)
          timestamp - actual time as seconds (integer)
  returns: true if signature is valid."
  [sig apikey secret timestamp]
  (= sig (make-sig apikey secret timestamp)))

(def
^{:doc "The token response map fixed values."}
  token_response_map
  {:return_type "json",
   :token_type "bearer",
   :expires_in 3600 ; in seconds
   })

(defn make-access-token
  "TODO generate the token using a key-pair."
  [client_params]
  "2tz9ebpzqmkyk2rdtg3q7tw9"
  )

(defn make-refresh-token
  [client_params]
  "s482jymwgm9z4a973h7h5qbq"
  )



(defn get-token-response
  [ctx]
  ;; (log/debugf "get-token-response: ctx = %s" ctx)
  (let [{:keys [response request parameters]} ctx
        sig (get-in parameters [:query :sig])
        {:keys [query-string body headers]} request
        apikey (get headers "X-Api-Key")
        clientparams (json/parse-string body true)]
    ;; (log/debugf "get-token-response: sig = %s" sig)
    ;; (log/debugf "get-token-response: apikey = %s" apikey)
    ;; (log/debugf "get-token-response: clientparams = %s" clientparams)
    (if (valid-sig? sig
                    apikey
                    (:client_secret clientparams)
                    (quot (tcc/to-long (tcor/now)) 1000))
    (assoc response :body (json/generate-string
                          (assoc token_response_map  
                                 :access_token (make-access-token clientparams)
                                 :refresh_token 
                                 )))
    (assoc response :status 400))))

 

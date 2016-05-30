(ns rebujito.api.sig
  (:require [buddy.sign.util :refer (to-timestamp)]
            [buddy.core.codecs :refer (bytes->hex)]
            [manifold.deferred :as d]
            [rebujito.api.time :as api-time]
            [buddy.core.hash :as hash])
  (:import [java.time ZonedDateTime]
           [java.time.temporal ChronoUnit]))

(defn new-sig
  ([api-key api-secret]
   (new-sig (api-time/now) api-key api-secret))
  ([^ZonedDateTime time api-key api-secret]
   (let [s (to-timestamp time)]
     (-> (hash/md5 (format "%s%s%s" api-key api-secret s))
         (bytes->hex)))))

(defn check
  "The sig (signature) value is calculated by generating an MD5 hash made up of the API key, the API user's shared secret, and a UNIX timestamp reflecting number of seconds since the Unix Epoch (January 1 1970 00:00:00 GMT) at the time the request was made. There is a 5 minute grace period to allow for clock drift between client and server."
  ([sign api-key api-secret]
   (check (api-time/now) sign api-key api-secret))
  ([^ZonedDateTime now sign api-key api-secret]
   (loop [grace-seconds 0]
     (let [t1 (.minus now grace-seconds ChronoUnit/SECONDS)
           t2 (.plus now grace-seconds ChronoUnit/SECONDS)
           past-sign (new-sig t1 api-key api-secret)
           next-sign (new-sig t2 api-key api-secret)]
       (if (or (= past-sign sign) (= next-sign sign))
         (do
           (println "found sign at " grace-seconds " seconds")
           true)
         (if (> (* 60 5) grace-seconds)
           (recur (inc grace-seconds))
           nil))))))

(defn deferred-check [sig client-id client-secret]
  (let [d* (d/deferred)]
    (future
      (if (check sig client-id client-secret)
        (d/success! d* "SUCCESS")
        (d/error! d* (ex-info (str "API ERROR!")
                              {:type :api
                               :status 400
                               :body (format "sign value: %s not valid " sig)}))))d*))

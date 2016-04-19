(ns ^:figwheel-always node-cljs.security
    (:require
     [node-cljs.time :refer (timestamp-in-seconds)]
     [cljs-hash.md5 :as md5]))


(defn encrypt [message]
  (md5/md5 message))

(defn sign [api-key api-pw]
  (encrypt (str api-key api-pw (timestamp-in-seconds))))

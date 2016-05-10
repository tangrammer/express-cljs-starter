(ns ^{:doc "Utilities"}
  starbucks.util
  (:require
   [byte-streams :as bs]
   [clojure.data.codec.base64 :as base64]
   [clojure.test :refer :all]))


(defn to-string [s]
  (bs/convert s String))

(defn encode-basic-authorization [user password]
  (str "Basic " (to-string (base64/encode (.getBytes (str user ":" password))))))

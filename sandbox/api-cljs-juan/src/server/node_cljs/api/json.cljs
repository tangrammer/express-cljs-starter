(ns node-cljs.api.json
  (:require [cognitect.transit :as t]))

(def r (t/reader :json))

(def w (t/writer :json))

(defn read [body]
  (t/read r body))

(ns node-cljs.api.error
  (:require
     [node-cljs.log :as log]
     [cuerdas.core :as cuerdas]))

(defn new-error [code message]
  (js/Error. (str  code "$" message)))

(defn read-error [e]
  (cuerdas/split (.-message e) #"\$"))

(defn send-error-response [e res]
  (let [[code message] (read-error e)]
    (log/error  "<<<< error getting data" code "****" message)
    (.status res code)
    (.send res message)))

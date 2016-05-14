(ns mimi.routes
  (:require [cljs.nodejs :as nodejs]
            [mimi.express :refer [app]]
            [mimi.log :as log]))

(def jwt (nodejs/require "express-jwt"))

; (.use app (.unless (jwt #js {:secret config/jwt-secret}) #js {:path #js ["/mimi/health"]}))

(.get app "/mimi/health" #(.send %2 "ok"))

(.post app "/mimi/starbucks/account"
  (fn
    [req res]
    "create a starbucks customer in micros"
    (let [body (.-body req)]
      (log/debug "gots" body)
      (.send res "oks"))))

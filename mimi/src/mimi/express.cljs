(ns mimi.express
  (:require [cljs.nodejs :as nodejs]
            [mimi.log :as log]
            [mimi.config :as config]))

(def express (nodejs/require "express"))
(def http (nodejs/require "http"))
(def morgan (nodejs/require "morgan"))
(def body-parser (nodejs/require "body-parser"))
(def jwt (nodejs/require "express-jwt"))

(def app (express))

(.use app (morgan "dev"))
(.use app (.json body-parser))
(.use app (.unless (jwt #js {:secret config/jwt-secret}) #js {:path #js ["/mimi/health"]}))

(.get app "/mimi/health" #(.send %2 "ok"))

(defn init []
  "bhauman's secret sauce http://bit.ly/1MZE1zx"
  (doto (.createServer http #(app %1 %2))
    (.listen config/port
      (fn []
        (log/info "mimi started on" config/port)))))

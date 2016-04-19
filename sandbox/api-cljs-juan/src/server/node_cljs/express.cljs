(ns ^:figwheel-always node-cljs.express
  (:require [cljs.nodejs :as nodejs]
            [node-cljs.config :as config]
            [node-cljs.log :as log]))

(def express (nodejs/require "express"))
(def http (nodejs/require "http"))
(def morgan (nodejs/require "morgan"))

(def app (express))

(.use app (morgan "dev"))

(defn init []
  "bhauman's secret sauce http://bit.ly/1MZE1zx"
  (doto (.createServer http #(app %1 %2))
    (.listen config/port
      (fn []
        (log/info "listening on" config/port)))))

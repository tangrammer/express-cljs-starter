(ns mimi.core
  (:require [cljs.nodejs :as nodejs]
            [mimi.log :as log]
            [mimi.config :refer [port]]
            [mimi.express :refer [app]]
            [mimi.routes]))

(nodejs/enable-util-print!)

(def http (nodejs/require "http"))

; bhauman's secret sauce http://bit.ly/1MZE1zx
(def -main
  (fn []
    (doto (.createServer http #(app %1 %2))
      (.listen port #(log/info "mimi started on" port)))))

(set! *main-cli-fn* -main)

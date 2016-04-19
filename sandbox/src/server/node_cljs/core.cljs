(ns ^:figwheel-always node-cljs.core
  (:require [cljs.nodejs :as nodejs]
            [node-cljs.log :as log]
            [node-cljs.routes :as routes]
            [node-cljs.express :as express]
            [figwheel.client :as fw]))

(enable-console-print!)

(def -main #(express/init)) ; http://bit.ly/1MZE1zx

(set! *main-cli-fn* -main)

(fw/start {})
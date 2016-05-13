(ns mimi.core
  (:require [cljs.nodejs :as nodejs]
            [mimi.express :as express]))

(nodejs/enable-util-print!)

(def -main #(express/init)) ; http://bit.ly/1MZE1zx

(set! *main-cli-fn* -main)

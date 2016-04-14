(ns ^:figwheel-always node-cljs.time
    (:require [cljs-time.core :as tc]))


(tc/second(tc/now))

(defn timestamp-in-seconds []
  (js/Math.floor (/ (.now js/Date) 1000)))

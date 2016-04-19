(ns node-cljs.log
  (:require  [taoensso.timbre :as log ]))

(log/set-config!  {:level :debug
                   :ns-blacklist  ["kvlt.*"]})

(def stdout js/console.log)

(def stderr js/console.error)

(defn logit [out-fn level & args]
  (apply out-fn (cons level args)))

(def debug (partial logit stdout "debug:"))

(def info (partial logit stdout "info:"))

(def error (partial stderr "error:"))

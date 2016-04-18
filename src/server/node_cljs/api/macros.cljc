(ns node-cljs.api.macros
  #? (:cljs
      (:require [cljs.core.async :as async :refer [put! chan <! >! close!]]))
  )

(defn error? [x]
  #? (:cljs
      (instance? js/Error x)))


(defn throw-err [e]
  #? (:clj
      (do
        (when (instance? Throwable e) (throw e))
        e)
      :cljs
      (if (instance? js/Error e)
        (throw e)
        e)

      ))




#?(:clj
   (defmacro <? [ch]
     `(throw-err (async/<! ~ch))))

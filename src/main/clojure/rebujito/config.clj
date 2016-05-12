(ns rebujito.config
  (:require [aero.core :as aero]))

(defn config
  ([]
   (config nil))
  ([profile]
   ;;(aero/read-config "src/main/resources/config.edn" {:profile profile})
    (aero/read-config (clojure.java.io/resource "config.edn") {:profile profile})

   ))

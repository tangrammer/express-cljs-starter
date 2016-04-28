(ns rebujito.config
  (:require [aero.core :as aero]))

(defn config
  ([]
   (config nil))
  ([profile]
   (aero/read-config "src/main/resources/config.edn" {:profile profile})))

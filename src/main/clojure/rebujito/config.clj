(ns rebujito.config
  (:require [aero.core :as aero]))


(defn config [profile]
  (aero/read-config "src/main/resources/config.edn" {:profile profile}))

(ns rebujito.api.resources.stores
  (:require
   [rebujito.api.util :as util]))

(defn by-region []
  (->
   {:methods
    {:get {:parameters {:query {:access_token String}}
           :response (slurp (clojure.java.io/resource "mocks/stores.json"))}}}
   (merge (util/common-resource :stores/by-region))))

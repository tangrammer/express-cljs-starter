(ns rebujito.api.resources.stores
  (:require
   [taoensso.timbre :as log]
   [rebujito.api.util :as util]
   [rebujito.store.mocks :as mocks]
   [cheshire.core :as json]
   [yada.resource :refer [resource]]))

(defn by-region []
  (->
   {:methods
    {:get {:parameters {:query {:access_token String}}
           :response (slurp (clojure.java.io/resource "mocks/stores.json"))}}}
   (merge (util/common-resource :stores/by-region))))

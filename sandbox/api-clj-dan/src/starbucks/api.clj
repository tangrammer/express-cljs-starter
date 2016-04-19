(ns starbucks.api
  (:require
   [bidi.bidi :refer [RouteProvider]]
   [clojure.tools.logging :refer :all]
   [clojure.java.io :as io]
   [com.stuartsierra.component :refer [using Lifecycle]]
   [hiccup.core :refer [html]]
   [schema.core :as s]
   [starbucks.resources :as res]
   [bidi.bidi :refer [routes-context]]
   [yada.yada :as yada :refer [yada]]))

(defn api [db]
  ["/starbucks"
   [["" (-> (res/new-index-resource db)
            (assoc :id ::index))]
    [["/" :entry] (-> (res/new-entry-resource db)
                      (assoc :id ::entry))]
    ["/oauth/token" (res/new-oauth-resource db)]
    ]])

(s/defrecord ApiComponent [db routes]
  Lifecycle
  (start [component]
    (assoc component
           :routes (api db)))
  (stop [component]))

(defn new-api-component []
  (->
   (map->ApiComponent {})
   (using [:db])))

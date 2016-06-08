(ns rebujito.api.resources.social-profile
  (:require
   [rebujito.protocols :as p]
   [rebujito.api.util :refer :all]
   [cheshire.core :as json]
   [schema.core :as s]
   [yada.resource :refer [resource]]))



(def schema {:put {:accountImageUrl String}})


(defn account [store]
  (->
   {:methods
    {:put {:parameters {:query {:access_token String}
                        :body (-> schema :put)}
           :consumes [{:media-type #{"application/json"}
                       :charset "UTF-8"}]

           :response (fn [ctx]
                       (condp = (get-in ctx [:parameters :query :access_token])
                         "111023" (>400 ctx ["No Request supplied" "Request was malformed. Must contain a body."])
                         "111033" (>400 ctx ["User does not exist" "User could not be found"])
                         "500" (>500 ctx ["Internal Server Error :( " "An unexpected error occurred processing the request"])
                         (>200 ctx ["OK"])))}}}

   (merge (common-resource :me/social-profile))
   (merge access-control)))

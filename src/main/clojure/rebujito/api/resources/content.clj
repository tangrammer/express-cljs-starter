(ns rebujito.api.resources.content
  (:require
   [schema.core :as s]
   [rebujito.api.util :as util]))

(defn terms []
  (->
   {:methods
    {:get {:parameters {:query {:access_token String
                                (s/optional-key :locale) String}}
           :response (slurp (clojure.java.io/resource "mocks/lorem_ipsum.html"))}}}
   (merge (util/common-resource :content/terms)
          {:produces "application/json"})))

(ns rebujito.api.resources.content
  (:require
   [rebujito.api.util :as util]))

(defn terms []
  (->
   {:methods
    {:get {:parameters {:query {:access_token String}
                        :path {:market String}}
           :response (slurp (clojure.java.io/resource "mocks/lorem_ipsum.html"))}}}
   (merge (util/common-resource :content/terms)
          {:produces "text/html"})))

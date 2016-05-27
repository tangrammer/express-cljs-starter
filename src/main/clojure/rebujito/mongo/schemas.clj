(ns rebujito.mongo.schemas
  (:require
   [schema.coerce :as sc]
   [schema.core :as s]
   [schema.macros :as sm]
   [swarm.coercion :refer (throw-coercer-exceptions)]
))


(s/defschema QueryExample
  {(s/optional-key :id) String
   (s/optional-key :_id) org.bson.types.ObjectId
   s/Keyword s/Any})

(def QueryExampleMapping
 {QueryExample
  (fn [x]
   (cond
     (and (map? x) (:id x))
     (try (-> x
          (assoc :_id (org.bson.types.ObjectId. (:id x)))
          (dissoc :id)))
    :otherwise x))})

(def query-by-example-coercer
  (throw-coercer-exceptions (sc/coercer QueryExample
                                        QueryExampleMapping)))

(ns rebujito.api.util)

(defn >base [ctx status body]
  (-> ctx :response (assoc :status status)
      (assoc :body body)))

(defn >400 [ctx body]
  (>base ctx 400 body))

(defn >404 [ctx body]
  (>base ctx 404 body))

(defn >403 [ctx body]
  (>base ctx 403 body))

(defn >500 [ctx body]
  (>base ctx 500 body))

(defn >201 [ctx body]
  (>base ctx 201 body))

(defn >200 [ctx body]
  (>base ctx 200 body))

(defn common-resource
  ([desc]
   (let [n (if (keyword? desc)
             (if (namespace desc)
               (clojure.string/join "/" [(namespace desc) (name desc)])
               (name desc))
             desc)]
    (common-resource n n)))
  ([desc swagger-tag]
   {:description desc
    :produces [{:media-type
                #{"application/json" "application/xml"}
                :charset "UTF-8"}]
    :swagger/tags (if (vector? swagger-tag)
                    swagger-tag [swagger-tag])}))
(def access-control
  {:access-control {}})

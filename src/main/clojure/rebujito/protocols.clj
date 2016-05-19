(ns rebujito.protocols
    (:refer-clojure :exclude (find)))

(defprotocol Store
  (get-card [this])
  (get-payment-method-detail [this])
  (post-payment-method [this])
  (get-payment-method [this])
  (post-token-resource-owner [this])
  (post-refresh-token [this]))


(defprotocol Mimi
  (create-account [this data]))

(defmulti db-find "dispatch on data meaning"
  (fn [mutable-storage data] (type data)))

(defmethod db-find :default [_ data]
 (throw (IllegalArgumentException.
          (str "Not ready to find using " (type data)))))



(defprotocol MutableStorage
  (find [this]
    "returns collection")
  (insert! [this data]
    "return true/false")
  (update! [this data]
    "return true/false")
  (get-and-update! [this data]
    "the result is the updated document")
  (get-and-insert! [this data]
    "the result is the udpated document"))

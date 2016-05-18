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


(defprotocol MutableStorage
  (find
    [this]
    [this data])
  (insert! [this data]
    "return true/false")
  (update! [this data]
    "return true/false")
  (get-and-update! [this data]
    "the result is the updated document")
  (get-and-insert! [this data]
    "the result is the udpated document"))

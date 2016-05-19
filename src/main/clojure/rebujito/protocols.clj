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
    ;;"returns collection"
    [this data]
    ;;returns single item or collection, depending data type
    ;; if data means an id should return the document or nil
    ;; else data means the query, and then it returns a list of
    ;; documents that match the query
    )
  (insert! [this data]
    "aysnc, return true/false")
  (update! [this data]
    "async, return true/false")
  (get-and-update! [this data]
    "sync, the result is the updated document")
  (get-and-insert! [this data]
    "sync, the result is the new document"))

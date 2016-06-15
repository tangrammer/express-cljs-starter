(ns rebujito.protocols
  (:refer-clojure :exclude (find send deref)))

(defprotocol Store
  (get-cards [this])
  (get-profile [this])
  (get-deferred-payment-method-detail [this data])
  (put-payment-method-detail [this data])
  (post-payment-method [this data])
  (get-payment-method [this])
  (post-token-resource-owner [this])
  (post-refresh-token [this]))

(defprotocol PaymentGateway
;  (ping [this data])
  (create-card-token [this data])
  (delete-card-token [this data])
  (execute-payment [this data]))

(defprotocol Mimi
  (create-account [this data])
  (register-physical-card [this data])
  (load-card [this card-number amount])
  (rewards [this data])
  )

(defprotocol Encrypter
  (sign [_ data])
  (check [this unhash hashed]))

(defprotocol Authorizer
  (grant [this data scopes])
  (verify [this token scope])
  (scopes [this token]))

(defprotocol Authenticator
  (read-token [this token])
  (generate-token [this data minutes]))

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
  (generate-id [this data]
    "generate id for this storage")
  (insert! [this data]
    "aysnc, return true/false")
  (update! [this data-query data-update]
    "async, return true/false")
  (get-and-update! [this data]
    "sync, the result is the updated document")
  (get-and-insert! [this data]
    "sync, the result is the new document")
  (update-by-id! [this id data]))

(defprotocol UserStore
  (add-new-payment-method [this oid p])
  (get-payment-method [this oid payment-method-id])
  (add-auto-reload [this oid payment-data data])
  (disable-auto-reload [this oid])
  )
(defprotocol ApiClient
  (login [this id pw]))

(defprotocol Counter
  (increment! [this counter-name])
  (deref [this counter-name]))

(defprotocol MailService
  (send [this data]))

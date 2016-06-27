(ns rebujito.protocols
  (:refer-clojure :exclude (find send deref)))


(defprotocol PaymentGateway
;  (ping [this data])
  (create-card-token [this data])
  (delete-card-token [this data])
  (execute-payment [this data]))

(defprotocol Mimi
  (create-account [this data])
  (register-physical-card [this data])
  (load-card [this card-number amount])
  (balances [this data])
  (get-history [this card-number])
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
  (add-autoreload-profile-card [this oid autoreload-profile-card])
  (disable-auto-reload [this oid card-id])
  (insert-card! [this oid card])
  (get-user-and-card [this card-number]
    "get card with card-number not the card-id, and without user-id.
     The query is a nested over :cards collection")
  )

(defprotocol UserPaymentMethodStore
  (add-new-payment-method [this oid p])
  (get-payment-method [this oid payment-method-id])
  (remove-payment-method [this oid payment-method])
  (update-payment-method [this oid payment-method])
  (get-payment-methods [this oid]))

(defprotocol UserAddressStore
  (insert-address [this oid address])
  (get-addresses [this oid])
  (get-address [this oid address-id])
  (remove-address [this oid address])
  (update-address [this oid address]))

(defprotocol TokenStore
  (invalidate [this user-id]))


(defprotocol ApiClient
  (login [this id pw]))

(defprotocol Counter
  (increment! [this counter-name])
  (deref [this counter-name]))

(defprotocol MailService
  (send [this data]))

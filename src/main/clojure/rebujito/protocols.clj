(ns rebujito.protocols)

(defprotocol Store
  (get-card [this])
  (get-payment-method-detail [this])
  (post-payment-method [this])
  (get-payment-method [this])
  (post-token-resource-owner [this])
  )

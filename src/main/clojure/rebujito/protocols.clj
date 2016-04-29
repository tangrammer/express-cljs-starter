(ns rebujito.protocols)

(defprotocol Store
  (get-card [this])
  (get-payment-method [this])
  (post-payment-method [this])
  )

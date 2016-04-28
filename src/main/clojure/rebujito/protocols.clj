(ns rebujito.protocols)

(defprotocol Auth
  (sign [this data expire-in])
  (unsign [this data]))

(defprotocol Store
  (get-card [this])
  (get-payment-method [this]))

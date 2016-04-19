(ns co.za.swarmloyalty.rebujito.protocols)

(defprotocol Auth
  (sign [this data])
  (unsign [this data]))

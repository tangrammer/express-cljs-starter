(ns rebujito.api.resources.card.reload
  (:require
   [selmer.parser :as selmer]))

(defn render-file [template-file data]
  (selmer/render-file template-file data))

(ns rebujito.template
  (:require
   [selmer.parser :as selmer]))

(defn render-file [template-file data]
  (selmer/render-file template-file data))

(ns commie.macros)

(defmacro testmac [body]
  `(str ~body " yep this works now"))

(defmacro <? [expr]
  `(commie.core/throw-if-error (cljs.core.async/<! ~expr)))
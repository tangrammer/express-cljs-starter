(ns ^:figwheel-always node-cljs.config)

(def port (or js/process.env.PORT 3000))

(def route-prefix "pants")

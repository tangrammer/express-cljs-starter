(ns ^:figwheel-always node-cljs.config)

(def host "http://localhost:")
(def port (or js/process.env.PORT 3000))

(def route-prefix "pants")

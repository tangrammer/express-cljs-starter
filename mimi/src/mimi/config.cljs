(ns mimi.config)

(def port (or js/process.env.PORT 3000))
(def jwt-secret "the dark horse on a whim is a sweet cellar")

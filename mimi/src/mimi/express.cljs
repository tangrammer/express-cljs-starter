(ns ^:figwheel-always mimi.express
  (:require [cljs.nodejs :as nodejs]
            [mimi.log :as log]))

(def express (nodejs/require "express"))
(def morgan (nodejs/require "morgan"))
(def body-parser (nodejs/require "body-parser"))

(def app (express))

(.use app (morgan "dev"))
(.use app (.json body-parser))

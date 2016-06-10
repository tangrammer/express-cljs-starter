(ns mimi.runners.node
  (:require
    [mimi.tests :as tests]))

(set! *main-cli-fn* tests/run)

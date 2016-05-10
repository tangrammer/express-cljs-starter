(defproject starbucks "0.1.0-SNAPSHOT"
  :description "Starbucks Integration"
  :dependencies
  [;; Infrastructure
   [org.clojure/clojure "1.8.0"]
   [com.stuartsierra/component "0.3.1"]
   [prismatic/schema "1.0.5"]
   [org.clojure/core.async "0.2.374"]
   [org.clojure/tools.namespace "0.2.11"]
   [aleph "0.4.1-beta5" :exclusions [org.clojure/clojure byte-streams]]
   [yada "1.1.3" :exclusions [clj-tuple riddley potemkin manifold]]

   ;; Logging
   [org.clojure/tools.logging "0.3.1"]
   [org.slf4j/jcl-over-slf4j "1.7.13"]
   [org.slf4j/jul-to-slf4j "1.7.13"]
   [org.slf4j/log4j-over-slf4j "1.7.13"]
   [ch.qos.logback/logback-classic "1.1.3" :exclusions [org.slf4j/slf4j-api]]

   ;; Config
   [aero "0.2.3"]

   ;; Misc
   [cheshire "5.6.1"]
   [clj-time "0.11.0"]
   [buddy/buddy-core "0.12.1"]]



  :pedantic? :abort

  :plugins [
            [lein-checkall "0.1.1" :exclusions [org.clojure/tools.namespace org.clojure/clojure]]
            [lein-ancient "0.6.7" :exclusions [org.clojure/clojure]]]


  :main starbucks.main

  :repl-options {:init-ns user
                 :welcome (println "Type (dev) to start")}

  :profiles {:dev {:dependencies [[pjstadig/humane-test-output "0.8.0"]
                                  [ring-mock "0.1.5" :exclusions [commons-codec]]]

                   :injections [(require 'pjstadig.humane-test-output)
                                (pjstadig.humane-test-output/activate!)]
                   :test-paths ["src" "test"]
                   :source-paths ["dev"]}

             :check {:global-vars {*warn-on-reflection* true}}})

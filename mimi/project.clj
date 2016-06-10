(defproject mimi "0.1.0-SNAPSHOT"

  :min-lein-version "2.5.3"

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.170"]
                 [prismatic/schema "1.1.1"]]

  :plugins [[lein-cljsbuild "1.1.1"]
            [lein-figwheel "0.5.0-2"]]

  :source-paths ["src"]

  :clean-targets ["target"]

  :hooks [leiningen.cljsbuild]

  :cljsbuild {:test-commands {"default" ["node" "target/test/index.js"]}
              :builds [{:id "dev"
                        :source-paths ["src" "test"]
                        :figwheel true
                        :compiler {
                                   :main mimi.core
                                   :output-to "target/dev/server.js"
                                   :output-dir "target/dev"
                                   :target :nodejs
                                   :optimizations :none
                                   :source-map true}}
                       {:id "test"
                        :source-paths ["src" "test"]
                        :compiler {:main mimi.runners.node
                                   :output-to "target/test/index.js"
                                   :output-dir "target/test"
                                   :target :nodejs
                                   :optimizations :simple}}
                       {:id "prod"
                        :source-paths ["src"]
                        :compiler {:output-to "target/prod/server.js"
                                   :output-dir "target/prod"
                                   :target :nodejs
                                   :optimizations :simple}}]})

(defproject mimi "0.1.0-SNAPSHOT"

  :min-lein-version "2.5.3"

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.170"]]

  :plugins [[lein-cljsbuild "1.1.1"]
            [lein-figwheel "0.5.0-2"]]

  :source-paths ["src"]

  :clean-targets ["target"]

  :cljsbuild {:builds [{:id "dev"
                        :source-paths ["src"]
                        :figwheel true
                        :compiler {:main mimi.core
                                   :output-to "target/dev/server.js"
                                   :output-dir "target/dev"
                                   :target :nodejs
                                   :optimizations :none
                                   :source-map true}}
                       {:id "prod"
                        :source-paths ["src"]
                        :compiler {:output-to "target/prod/server.js"
                                   :output-dir "target/prod"
                                   :target :nodejs
                                   :optimizations :simple}}]})

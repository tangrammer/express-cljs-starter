(defproject commie "0.1.0-SNAPSHOT"

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.8.40"]
                 [org.clojure/core.async "0.2.374" :exclusions [org.clojure/tools.reader]]
                 [com.cognitect/transit-cljs "0.8.237"]]

  :plugins [[lein-figwheel "0.5.2"]
            [lein-cljsbuild "1.1.3" :exclusions [[org.clojure/clojure]]]]

  :clean-targets ["target"]

  :cljsbuild {
              :builds [{:id "dev"
                        :source-paths ["sandbox"]
                        :figwheel true
                        :compiler {:main "commie.core"
                                   :output-to "target/index.js"
                                   :output-dir "target/dev"
                                   :target :nodejs
                                   :optimizations :none
                                   :source-map true}}
                       {:id "prod"
                        :source-paths ["sandbox"]
                        :figwheel false
                        :compiler {:main "commie.core"
                                   :output-to "target/index.js"
                                   :output-dir "target/prod"
                                   :target :nodejs
                                   :optimizations :none
                                   :source-map true}}]})

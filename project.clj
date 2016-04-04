(defproject node-cljs "0.1.0-SNAPSHOT"

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.8.40"]]

  :npm {:dependencies [[source-map-support "0.4.0"]
                       [express "4.13.4"]
                       [morgan "1.7.0"]
                       [ws "1.0.1"]]}

  :plugins [[lein-cljsbuild "1.1.3"]
            [lein-figwheel "0.5.2"]
            [lein-npm "0.6.2"]]

  :source-paths ["target/server" "target/classes"]

  :clean-targets ["target"]

  :cljsbuild {
    :builds [{:id "server-dev"
              :source-paths ["src"]
              :compiler {:main node-cljs.core
                         :output-to "target/server/index-dev.js"
                         :output-dir "target/server"
                         :target :nodejs
                         :optimizations :none
                         :source-map true
                         :cache-analysis true}}
             {:id "server-prod"
              :source-paths ["src"]
              :compiler {:main node-cljs.core
                         :output-to "target/server/index.js"
                         :output-dir "target/server"
                         :target :nodejs
                         :optimizations :simple}}
             ]}
  ; :figwheel {}
  )
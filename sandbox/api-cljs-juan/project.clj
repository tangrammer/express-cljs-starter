(defproject rebujito "0.1.0-SNAPSHOT"

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.8.40"]
                 [cljs-hash "0.0.2"]
                 [com.andrewmcveigh/cljs-time "0.4.0"]
                 [org.clojure/core.async "0.2.374"]
                 [io.nervous/kvlt "0.1.1"]
                 [com.cognitect/transit-cljs "0.8.237"]
                 [prismatic/schema "1.1.0"]
                 [prismatic/plumbing "0.5.3"]
                 [bidi "2.0.6"]
                 [funcool/cuerdas "0.7.1"]
                 ]

  :npm {:dependencies [[source-map-support "0.4.0"]
                       [express "4.13.4"]
                       [morgan "1.7.0"]
                       [ws "1.0.1"]]}

  :plugins [[lein-cljsbuild "1.1.3"]
            [lein-figwheel "0.5.2"]
            [lein-npm "0.6.2"]]

  :source-paths ["src/server"]

  :clean-targets ["target"]

  ;; this is for developing using CIDER
  :profiles {:dev {:dependencies [[com.cemerick/piggieback "0.2.1"]
                                 [figwheel-sidecar "0.5.0-1"]]
                   :source-paths ["src/server" "dev"]
                   :plugins [[cider/cider-nrepl "0.10.1"]] }
             :repl {:plugins [[cider/cider-nrepl "0.10.1"]] }} ; <-- Note this


  :cljsbuild {
    :builds [{:id "server-dev"
              :source-paths ["src/server"]
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

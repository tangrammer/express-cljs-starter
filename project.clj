(defproject za.co.swarmloyalty/rebujito "0.1.0-SNAPSHOT"
  :description "starbucks api"
  :url "https://github.com/naartjie/rebujito"
  :license {:name "Proprietary"}
  :resource-paths ["src/main/resources"]
  :source-paths ["src/main/clojure"]
  :test-paths ["src/test/clojure"]

  :main ring.rebujito

  :dependencies [[org.clojure/clojure "1.8.0"]


                 ;; http server
                 [aleph "0.4.1" :exclusions [org.clojure/clojure]]
                 [bidi "2.0.6" :exclusions [ring/ring-core]]
                 [ring/ring-core "1.4.0"]
                 [yada "1.1.6" :exclusions [bidi org.clojure/clojurescript]]


                 ;; components / modular
                 [com.stuartsierra/component "0.3.1"]
                 [juxt.modular/aleph "0.1.4"]
                 [juxt.modular/bidi "0.9.5"]
                 [juxt.modular/ring "0.5.3"]



                 ;; data / data manipulation
                 [cheshire "5.5.0"]
                 [com.cognitect/transit-clj "0.8.285"]
                 [org.clojure/data.xml "0.0.8"]



                 ;; config loader
                 [tangrammer/aero "0.2.3"]


                 [potemkin "0.4.3" :exclusions [riddley]]
                 [prismatic/plumbing "0.5.3" :exclusions [prismatic/schema]]
                 [prismatic/schema "1.1.0" :exclusions [potemkin]]
                 [prismatic/schema-generators "0.1.0"]

                 ;; swagger-ui
                 [metosin/ring-swagger "0.22.6" :exclusions [potemkin]]
                 [metosin/ring-swagger-ui "2.1.4-0"]
                 [org.webjars/swagger-ui "2.1.4"]
                 [org.webjars/jquery "2.1.4"]
                 [org.flatland/ordered "1.5.3"]
                 [javax.servlet/servlet-api "2.5"]

                 ;; logging + profiling
                 [org.slf4j/slf4j-api "1.7.21"]
                 [com.taoensso/timbre "4.1.4"]
                 [com.fzakaria/slf4j-timbre "0.3.1" :exclusions [com.taoensso/timbre junit]]
                 [org.slf4j/jcl-over-slf4j "1.7.21"]
                 [org.slf4j/jul-to-slf4j "1.7.21"]
                 [org.slf4j/log4j-over-slf4j "1.7.21"]
                 [org.slf4j/slf4j-nop "1.7.21"]

                 ;; explicit deps to avoid conflicts
                 [clj-time "0.11.0"]
                 [org.clojure/tools.reader "1.0.0-beta1"]]

  :uberjar-name "rebujito.jar"
  :plugins [[lein-ring "0.9.7"]]


  :profiles {:uberjar {:main       ring.rebujito
                       :aot        :all}

             :dev     {:dependencies   [[org.clojure/tools.nrepl "0.2.11"]
                                        [org.clojure/tools.namespace "0.2.11"]]
                       :main user
                       :resource-paths ["src/main/resources" "src/test/resources"]

                       :source-paths   ["src/dev/clojure"]}})

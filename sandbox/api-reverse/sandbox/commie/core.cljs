(ns commie.core
  (:require [cljs.nodejs :as nodejs]
            [cljs.core.async :refer [<! chan close! put!]]
            [cognitect.transit :as transit]
            [commie.log :as log])

  (:require-macros [commie.macros :refer [testmac <?]]
                   [cljs.core.async.macros :refer [go]]))

(nodejs/enable-util-print!)

(defonce express (nodejs/require "express"))
(defonce body-parser (nodejs/require "body-parser"))
(defonce http (nodejs/require "http"))
(defonce request (nodejs/require "request"))
(defonce jwt (nodejs/require "express-jwt"))
(defonce morgan (nodejs/require "morgan"))

(def json-reader (transit/reader :json))

(def app (express))

(.use app (morgan "dev"))
(.use app (.json body-parser))
(.use app (.urlencoded body-parser))

(defn is-error? [x]
  (instance? js/Error x))

(defn throw-if-error [x]
  (if (is-error? x)
    (throw x)
    x))

(defn <json-get [url]
  "http get JSON --> returns EDN"
  (let [out (chan)
        handler (fn [err res body]
                  (if-let [result (transit/read json-reader body)]
                    (put! out result))
                  (close! out))]
    (request url handler)
    out))

(defn log-body [req res next]
  (print "request body" (.-body req))
  (next))

(.use app log-body)

(.get app "/health" #(.send %2 "oks"))

(.post app "/starbucks/v1/oauth/token" (fn [req res]
                                   (.json res (clj->js {:return_type "json"
                                                        :access_token "--hello-masiak--"
                                                        :token_type "bearer"
                                                        :expires_in 3600
                                                        :refresh_token "-----refresh---token------"
                                                        :scope nil
                                                        :state nil
                                                        :uri nil
                                                        :extended nil}))))

(.use app "/starbucks/v1/content/" (fn [req res]
                                     ; (.set res "Content-Type" "text/html")
                                     (.send res "<html><body><h1>Fake Content</h1></body></html>")))

(.get app "/starbucks/v1/me/profile" (fn [req res]
                                        (.json res (nodejs/require "../../me_profile.json"))))

(defn env [] (or (.-env.NODE_ENV js/process) "development"))

(def -main
  (fn []
    (doto (.createServer http #(app %1 %2))
      (.listen 3000 #(log/info (str "commie started on port 3000 [env:" (env) "]"))))))

(set! *main-cli-fn* -main)

(ns ^:figwheel-always node-cljs.routes
    (:require-macros [cljs.core.async.macros :refer [go]])
    (:require
     [cljs.core.async :as async :refer [put! chan <! >! close!]]
     [node-cljs.express :refer (app)]
     [kvlt.core :as kvlt]
     [kvlt.chan :as kvlt.chan]
     [promesa.core :as p]
     [bidi.bidi :as bidi]
     [cognitect.transit :as t]
     [node-cljs.log :as log]
     [node-cljs.time :as time]
     [node-cljs.api.card :as card]
     [node-cljs.config :as config]
     [node-cljs.security :as security]
     [node-cljs.api.utils :as api.utils :refer (GET POST #_defroute defroute* resource )]
     [schema.core :as s :include-macros true]
     ))


(def r (t/reader :json))
(def w (t/writer :json))

(.use app
      (fn [req res next]
        (log/debug "start" (.-method req) (.-path req))
        (next)))



(def mock-oauth (resource {:methods {:get
                                     {:parameters {:query {:sign s/Str}}
                                      :response  (fn [ctx]
                                                   {:sign (-> ctx :req :query :sign)})}}}))

(def oauth-server-url (str "http://localhost:" config/port "/mock/oauth"))

(def oauth-token (resource
                           {:methods {:get {:response (fn [ctx]
                                                        (go
                                                          (let [sign (security/sign "api-key" "api-pw")
                                                                {:keys [status success body] :as m}  (<! (kvlt.chan/request! {:url (str oauth-server-url "?sign=" sign)}))]

                                                            {:sign sign
                                                             :status status
                                                             :success success
                                                             :body  (t/read r body)})))}}}))

(def create-digital-card (resource {:description "Create digital card"
                                    :produces [{:media-type
                                                #{"application/xml;q=0.8" "application/json;q=0.8"}
                                                :charset "UTF-8"}]
                                    :methods
                                    {:post {:parameters {:query {:access_token s/Str}}
                                            :consumes [{:media-type #{"application/xml" "application/json"}
                                                        :charset "UTF-8"}]
                                            :response
                                            (fn [ctx]
                                              (go
                                                (let [sign (security/sign "api-key" "api-pw")
                                                      {:keys [status success body]}  (<! (kvlt.chan/request! {:url (str oauth-server-url "?sign=" sign)}))
                                                      ]
                                                  (clj->js (merge
                                                            {:status status
                                                             :access-token (-> ctx :req :query :access_token)}
                                                            (card/data)))))
                                              )}}}))

(reset! api.utils/routes ["/" {"me/cards/register-digital" create-digital-card
                               "v1/oauth/token" oauth-token
                               "mock/oauth" mock-oauth}])


(.use app api.utils/default-route)

(ns ^:figwheel-always node-cljs.routes
    (:require-macros [cljs.core.async.macros :refer [go]])
    (:require
     [cljs.core.async :as async :refer [put! chan <! >! close!]]
     [node-cljs.express :refer (app)]
     [kvlt.core :as kvlt]
     [kvlt.chan :as kvlt.chan]
     [promesa.core :as p]
     [bidi.bidi :as bidi]
     [node-cljs.log :as log]
     [node-cljs.time :as time]
     [node-cljs.api.card :as card]
     [node-cljs.api.json :as json]
     [node-cljs.config :as config]
     [node-cljs.security :as security]
     [node-cljs.api.utils  :refer (default-route routes resource )]
     [schema.core :as s :include-macros true]))

(.use app
      (fn [req res next]
        (log/debug "start" (.-method req) (.-path req))
        (next)))

(def mock-oauth-url (str config/host config/port "/mock/oauth"))


(def mock-oauth (resource {:methods {:get
                                     {:parameters {:query {:sign s/Str}}
                                      :response  (fn [ctx]
                                                   {:sign (-> ctx :req :query :sign)})}}}))



(def oauth-token (resource
                           {:methods {:get {:response (fn [ctx]
                                                        (go
                                                          (let [sign (security/sign "api-key" "api-pw")
                                                                {:keys [status success body] :as m}  (<! (kvlt.chan/request! {:url (str mock-oauth-url "?sign=" sign)}))]

                                                            {:sign sign
                                                             :status status
                                                             :success success
                                                             :body  (json/read body)})))}}}))

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
                                                      {:keys [status success body]}  (<! (kvlt.chan/request! {:url (str mock-oauth-url "?sign=" sign)}))
                                                      ]
                                                  (clj->js (merge
                                                            {:status status
                                                             :access-token (-> ctx :req :query :access_token)}
                                                            (card/data)))))
                                              )}}}))

(reset! routes ["/" {"me/cards/register-digital" create-digital-card
                               "v1/oauth/token" oauth-token
                               "mock/oauth" mock-oauth}])

(.use app  default-route)

(ns ^:figwheel-always node-cljs.routes
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [cljs.core.async :as async :refer [put! chan <! >! close!]]
     [node-cljs.express :refer (app)]
     [kvlt.core :as kvlt]
     [kvlt.chan :as kvlt.chan]
     [promesa.core :as p]
     [cognitect.transit :as t]
     [node-cljs.log :as log]
     [node-cljs.time :as time]
     [node-cljs.config :as config]
     [node-cljs.security :as security]
     [node-cljs.api.utils :as api.utils :refer (GET POST defroute defroute*)]
     ))


(def r (t/reader :json))
(def w (t/writer :json))
(.use app
      (fn [req res next]
        (log/debug "start" (.-method req) (.-path req))
        (next)))

(.get app "/" #(.send %2 "home"))

(defroute GET "/v1/customer/:customerId"
  (fn [req res]
    #js {:customer (-> req :params :customerId)}))

(.get app "/1" api.utils/path-info)
(.get app "/2" api.utils/path-info)
(.get app "/3" api.utils/path-info)



(defroute GET "/mock/oauth"
  (fn [req res]
    #js {:sign (-> req :query :sign)}))

(def oauth-server-url (str "http://localhost:" config/port "/mock/oauth"))

(defroute* GET "/v1/oauth/token"
  (fn [req res]
    (go
      (let [sign (security/sign "api-key" "api-pw")
            {:keys [status success body]}  (<! (kvlt.chan/request! {:url (str oauth-server-url "?sign=" sign)}))]
        (clj->js {:sign sign
                  :status status
                  :success success
                  :body  (clj->js (t/read r body))})))))


(.use app api.utils/default-route)

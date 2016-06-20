(ns rebujito.util
  (:require [taoensso.timbre :as log]
            [rebujito.api.util :as u]))
(def send-bugsnag true)
(defmacro local-context []
  (let [symbols (keys &env)]
    (zipmap (map (fn [sym] `(quote ~sym)) symbols) symbols)))

(defmacro ex? [t m]
  `(condp = ~t
     :store (rebujito.MongoException. ~m)
     :mimi (rebujito.MimiException. ~m)
    (Exception. ~m)))

(defmacro error* [t status id message ks]
  `(let [ex# {:context  (select-keys (local-context)  (quote ~('map 'symbol ks)))
              :status ~status
              :body (str (namespace ~id) "/" (name ~id))
              :message ~message}
         ctx# (or (:ctx (local-context)) {})]
     (if send-bugsnag
       (do
        (clj-bugsnag.core/notify
         (ex? ~t (str ~status " :: " (:body ex#) " :: " (name ~message)))
         (let [email# (or (:emailAddress (rebujito.api.util/authenticated-user ctx#))
                          "unauthenticated user")]
           {:api-key (:key (:bugsnag (rebujito.config/config)))
            :meta {:status (-> ctx# :response :status)
                   :context (assoc (:context ex#)
                                   :fn (:body ex#))
                   :id (:body ex#)
                   :uri (-> ctx# :request :uri)
                   :parameters (-> ctx# :parameters)
                   :email email#
                   }
            :user {:id (:body ex#)
                   :uri (-> ctx# :request :uri)
                   :parameters (-> ctx# :parameters)
                   :email email#}})))
       (log/error "AVOID SENDING BUGSNAG" (str ~status " :: " (:body ex#) " :: " (name ~message)))
       )

     (manifold.deferred/error-deferred (clojure.core/ex-info (str ~status " :: " (:body ex#) " :: " (name ~message))
                                                             {:type ~t
                                                              :status ~status
                                                              :body (str ~status " :: " (:body ex#) " :: " (name ~message))
                                                              :message (str ~status " :: " (:body ex#) " :: " (name ~message))}))))




(comment
  (let [c 3
       d 7]
   (error* :store 500 :resouid "e exception message" [c d])
   )

  )

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

(defmacro >take [local-context k]
  `(do
     ;;    (println (symbol  (name '~k)))
     ;;    (println (keys ~local-context))
     (get ~local-context  (symbol (name (quote ~k))))))


(defmacro >select [local-context ks more]
  `(do
     (println "more" ~more (select-keys ~local-context   ~('map 'symbol more)))
     ;;    (println (symbol  (name '~k)))
     ;;    (println (keys ~local-context))
     (let [r# (select-keys ~local-context  (quote ~('map 'symbol ks)))]
       (println "(some? r#)" (empty? r#) r#)
       (if (empty? r#)
         (select-keys ~local-context  ~('map 'symbol more))
         r#))))

(macroexpand-1 '(macroexpand-1 '(>take {:hola 1} hola)))

(macroexpand '(>take {:hola 1} hola))

;(println (>take {'hola 1} hola))
(defmacro error* [status message & ks]

  `(let [local-context# (local-context)
         a# (println '~ks)

         type# (or (>take local-context# try-type) :default-error-type)
         id# (or (>take local-context# try-id) 'macro-error-default-id)
         ex# {:context  (>select local-context#  ~ks (>take local-context# try-context))
              :status ~status
              :body (str (namespace id#) "/" (name id#))
              :message ~message}
         ctx# (or (:ctx local-context#) {})]
     (if send-bugsnag
       (do
        (clj-bugsnag.core/notify
         (ex? type# (str ~status " :: " (:body ex#) " :: " (name ~message)))
         (let [email# {:email  (or (:emailAddress (rebujito.api.util/authenticated-user ctx#))
                                   "unauthenticated user")}]
           {:api-key (:key (:bugsnag (rebujito.config/config)))
            :meta (merge {:context (assoc (:context ex#)
                                          :fn (:body ex#))
                          :id (:body ex#)}
                         (when (-> ctx# :response :status)
                           {:status (-> ctx# :response :status)})
                         (when (-> ctx# :request :uri)
                           {:uri (-> ctx# :request :uri)})
                         (when (-> ctx# :parameters)
                           {:parameters (-> ctx# :parameters)})
                          email#
                         )
            :user (merge
                   {:id (:body ex#)}
                   (when (-> ctx# :request :uri)
                     {:uri (-> ctx# :request :uri)})
                   (when (-> ctx# :parameters)
                     {:parameters (-> ctx# :parameters)})

                   email#)})))
       (log/error "AVOID SENDING BUGSNAG" (str ~status " :: " (:body ex#) " :: " (name ~message))))
     (manifold.deferred/error-deferred (clojure.core/ex-info (str ~status " :: " (:body ex#) " :: " (name ~message))
                                                             {:type type#
                                                              :status ~status
                                                              :context (assoc (:context ex#)
                                                                              :fn (:body ex#))
                                                              :keys   (quote ~('map 'symbol ks))
                                                              :context-keys   (keys local-context#)
                                                              :body (str ~status " :: " (:body ex#) " :: " (name ~message))
                                                              :message (str ~status " :: " (:body ex#) " :: " (name ~message))}))))

(comment
  (let [c 3
       d 7]
   (error* :store 500 :resouid "e exception message" [c d])
   ))

(defmacro t* [ks]
  `(quote ~ks)
  )

(defmacro dtry [body & context-kw]
  `(try
     ~@body
     (catch Exception error# (error* 500  (.getMessage error#) ~@('map 'symbol context-kw)))))


(defmacro hola* [a b & body]
  `(do
     (println ~(nil? body))
    )
  )

(hola* :a :b)

(macroexpand-1 '(t* [a b]))

(t* [a b])





(->
 (let [
        try-type :store
        try-id ::add-new-payment-method
       try-context '[oid p jolin]
       oid 12
       jolin 4
       p {:a 12}]

   (dtry (do
          ;;(println 'try-type)
;           (throw (Exception. "wow!"))
           (error* 400  :transaction-failed  oid))))
 (manifold.deferred/catch  Exception #(ex-data %))
 )



#_(-> (let [try-type :store
          try-id ::add-new-payment-method
          try-context '[oid p]
          oid 12
          p {:a 12}]
      (error* 400  :transaction-failed
              ))
    (manifold.deferred/catch  Exception #(ex-data %))
    )

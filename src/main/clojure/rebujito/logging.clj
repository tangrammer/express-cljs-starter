(ns rebujito.logging
  (:require [taoensso.timbre :as timbre]
            [taoensso.timbre.appenders.core :as appenders]
            [clojure.string :as str]))

(defn default-output-fn
  "Default (fn [data]) -> string output fn.
  You can modify default options with `(partial default-
output-fn <opts-map>)`."
  ([data] (default-output-fn nil data))
  ([{:keys [no-stacktrace? stacktrace-fonts] :as opts} data]
   (let [{:keys [level ?err_ vargs_ msg_ ?ns-str hostname_ timestamp_]} data]
     (str "Rebujito!"
             (force timestamp_)       " "
;             (force hostname_)        " "
       (str/upper-case (name level))  " "
       "[" (or ?ns-str "?ns") "] - "
       (force msg_)
       (when-not no-stacktrace?
         (when-let [err (force ?err_)]
           (str "\n" (timbre/stacktrace err opts))))))))

(def timbre-rebujito-config
  {:level :info ; e/o #{:trace :debug :info :warn :error :fatal :report}

   ;; Control log filtering by namespaces/patterns. Useful for turning off
   ;; logging in noisy libraries, etc.:

   :ns-blacklist  []    ;  #_["taoensso.*"]

   :middleware []           ; (fns [data]) -> ?data, applied left->right

   :timestamp-opts  timbre/default-timestamp-opts    ; {:pattern _ :locale _ :timezone _}

   :output-fn timbre/default-output-fn    ; (fn [data]) -> string

   :appenders
   {:println (timbre/println-appender {:stream :auto})
    :spit (appenders/spit-appender {:fname "./timbre-rebujito-spit.log"})}})


(def timbre-info-config
  {:level :info ; e/o #{:trace :debug :info :warn :error :fatal :report}

   ;; Control log filtering by namespaces/patterns. Useful for turning off
   ;; logging in noisy libraries, etc.:
   :ns-whitelist  []

   :ns-blacklist  ["org.apache.*" ] #_["taoensso.*"]

   :middleware []           ; (fns [data]) -> ?data, applied left->right

   :timestamp-opts  timbre/default-timestamp-opts    ; {:pattern _ :locale _ :timezone _}

   :output-fn timbre/default-output-fn    ; (fn [data]) -> string

   :appenders
   {:println (timbre/println-appender {:stream :auto})

     :spit (appenders/spit-appender {:fname "./timbre-rebujito-spit.log"})}})



(def timbre-debug-config
  {:level :debug ; e/o #{:trace :debug :info :warn :error :fatal :report}

   ;; Control log filtering by namespaces/patterns. Useful for turning off
   ;; logging in noisy libraries, etc.:

   :ns-blacklist  [] #_["taoensso.*" "org.apache.cxf.binding.*"]

   :middleware []           ; (fns [data]) -> ?data, applied left->right

   :timestamp-opts  timbre/default-timestamp-opts    ; {:pattern _ :locale _ :timezone _}

   :output-fn timbre/default-output-fn    ; (fn [data]) -> string

   :appenders
   {:println (timbre/println-appender {:stream :auto})

     :spit (appenders/spit-appender {:fname "./timbre-rebujito-spit.log"})}})



(def timbre-error-config
  {:level :error ; e/o #{:trace :debug :info :warn :error :fatal :report}

   ;; Control log filtering by namespaces/patterns. Useful for turning off
   ;; logging in noisy libraries, etc.:

   :ns-blacklist  ["o.a.c.c.*" "o.*" "taoensso.*" "org.apache.cxf.binding.*" ] #_["taoensso.*" "org.apache.cxf.*"]

   :middleware []           ; (fns [data]) -> ?data, applied left->right

   :timestamp-opts  timbre/default-timestamp-opts    ; {:pattern _ :locale _ :timezone _}

   :output-fn timbre/default-output-fn    ; (fn [data]) -> string

   :appenders
   {:println (timbre/println-appender {:stream :auto})

     :spit (appenders/spit-appender {:fname "./timbre-rebujito-spit.log"})}})



;(timbre/set-config! timbre-info-config)

;(timbre/set-config! dev-config)
;(timbre/debug "Timbre configured!")

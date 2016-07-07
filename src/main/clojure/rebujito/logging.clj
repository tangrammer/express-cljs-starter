(ns rebujito.logging
  (:require [taoensso.timbre :as timbre]
            [taoensso.timbre.appenders.core :as appenders]
            [clojure.string :as str]
            [taoensso.encore    :as enc :refer (compile-if have have? qb)]
            [taoensso.timbre.profiling :as profiling
             :refer (pspy p defnp profile)])
  )

(timbre/level>=  :debug :warn)

(def config-levels [["rebujito.api.util.*" :info]])

(defn max-level [levels]
  (reduce (fn [x1 x2] (if (timbre/level>= x1 x2) x1 x2)) (first timbre/ordered-levels) levels))

(defn min-level [levels]
  (reduce (fn [x1 x2] (if (timbre/level>= x1 x2) x2 x1)) (first levels) (next levels)))

;; (max-level  (map last [[#"^rebujito\.api\.(.*)$" :info] [#"^rebujito\.api\.util$" :warn]]))

(defn x? [x]
  (enc/cond!
   (enc/re-pattern? x) x
   (string? x)
   (let [s (-> (str "^" x "$")
               (str/replace "." "\\.")
               (str/replace "*" "(.*)"))]
     (re-pattern s))))

(defn log? [[ns? level?] config-levels]
  (let [->re-pattern (fn [[x l]] [(x? x) l])
        coincidences (mapv ->re-pattern config-levels)
        coincidences (->> coincidences
                          (map (fn [[n l]] [(re-find n ns?) l]) )
                          (filter (fn [[x _]] (some? x))))
        max-level* (max-level (map last coincidences))
        min-level* (min-level (map last coincidences))]
;     (println "hhha" coincidences max-level* min-level*)
     (let [[r l] [(first coincidences) min-level*]]
       (or (nil? r) (timbre/level>= level? l)))))

(def cf [["rebujito.api.util" :debug]
         ["rebujito.api.*" :warn]])



(assert (false? (log? ["rebujito.api.util" :trace] cf)))
(assert (false? (log? ["rebujito.api.util" :trace] (reverse cf))))

(assert (log? ["rebujito.api.util" :debug] cf))
(assert (log? ["rebujito.api.util" :debug] (reverse cf)))

(assert  (log? ["rebujito.api.util" :info] cf))
(assert  (log? ["rebujito.api.util" :info] (reverse cf)))

(assert (false? (log? ["rebujito.api.lolo" :info] cf)))

(assert (false? (log? ["rebujito.api.lolo" :info] (reverse cf))))


(assert (log? ["rebujito.api.lolo" :warn] cf))
(assert (log? ["rebujito.api.lolo" :warn] (reverse cf)))


(assert (log? ["rebujito.api.lolo" :error] cf))
(assert (log? ["rebujito.api.lolo" :error] (reverse cf)))









#_(profile :info :GAU
         (dotimes [n 10000]
           (log? [(str "rebujito.api.util." n) :warn] config-levels)))

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

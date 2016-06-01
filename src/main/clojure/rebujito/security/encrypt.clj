(ns rebujito.security.encrypt
  (:require [com.stuartsierra.component :refer [system-map system-using using] :as component]
            [buddy.core.mac :as mac]
            [buddy.core.codecs :refer (bytes->hex)]
            [rebujito.protocols :as p]))

(defn verify [pass hash secret-key]
  (mac/verify pass hash {:key secret-key :alg :hmac+sha256}))

(defn hash-password [user-pw secret-key]
  (mac/hash user-pw {:key secret-key
                     :alg :hmac+sha256}))


(verify "asd"  (hash-password "asd" "xxx") "xxx")


(defrecord SHA256Encrypter [secret-key]
  component/Lifecycle
  (start [this]
    this)
  (stop [this] this)
  p/Encrypter
  (sign [_ data]
    (bytes->hex (hash-password data secret-key)))
  (check [this unhash hashed]
      (= hashed (p/sign this unhash))


    ))

(defn new-sha256-encrypter [config]
  (map->SHA256Encrypter config))

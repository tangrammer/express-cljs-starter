(ns rebujito.api.time
  (:require [buddy.sign.util :refer (to-timestamp)])
  (:import [java.time ZonedDateTime]
           [java.time.temporal ChronoUnit]
           [java.time ZoneId]
           [java.util TimeZone])
  )

(def zone-id (ZoneId/of "Z"))

(def time-zone (TimeZone/getTimeZone zone-id))

(println "default time zone!: =>"(str  time-zone))

(extend-protocol buddy.sign.util/ITimestamp
  ZonedDateTime
  (to-timestamp [obj]
    (.toEpochSecond obj)))

(defn now
  ([]
   (now zone-id))
  ([zone-id]
   (ZonedDateTime/now zone-id)))

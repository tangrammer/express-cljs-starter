(ns rebujito.api.time
  (:require [buddy.sign.util :refer (to-timestamp)]
            [clj-time.core :as t]
            [clj-time.format :as f])
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

(def iso-date-format (f/formatter "yyyy-MM-dd"))

(defn one-year-from [date]
 (f/unparse
   iso-date-format
   (t/plus
     (f/parse iso-date-format date)
     (t/years 1))))

(defn today []
  (f/unparse iso-date-format (t/now)))

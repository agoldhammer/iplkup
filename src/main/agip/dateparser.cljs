(ns agip.dateparser
  (:require [tick.core :as t]))

(def month-map
  {"Jan" "01" "Feb" "02" "Mar" "03" "Apr" "04" "May" "05" "Jun" "06"
   "Jul" "07" "Aug" "08" "Sep" "09" "Oct" "10" "Nov" "11" "Dec" "12"})

(defn- datestr->yyyyMMdd
  "converts date string 27/Feb/2021:00:58:22 from log to yyyy-MM-ddThh:MM:ss string"
  [datestr]
  (let [re #"(\S+)/(\S+)/(\S+?):(\S+):(\S+):(\S+)"
        parsed (re-find re datestr)]
    (str (parsed 3) "-" (get month-map (parsed 2)) "-"
         (parsed 1) "T" (parsed 4) ":" (parsed 5) ":"
         (parsed 6))))

(defn datestr->zulu
  "converts log date str 27/Feb/2021:00:58:22 to offset-by 0 tick/date-time"
  [datestr]
  (t/offset-by
   (-> datestr
       (datestr->yyyyMMdd)
       (t/date-time)) 0))

(comment
  (datestr->zulu "27/Feb/2021:00:58:22") )

(comment (datestr->yyyyMMdd "27/Feb/2021:00:58:22")
         (t/offset-by (t/date-time (datestr->yyyyMMdd "27/Feb/2021:00:58:22")) 0)
         (let [odt (t/offset-by (t/date-time (datestr->yyyyMMdd "27/Feb/2021:00:58:22")) 0)]
           (t/format odt)))

(ns agip.logparse
  (:require ["fs" :as fs]
            [clojure.string :as s]))

(defn read-log
  "read log file"
  [fname]
  (-> (fs/readFileSync fname)
      (.toString)
      (s/split-lines)))

(defn parse-line
  "parse line of log, returns basic log entry
   w/o augmented data"
  [line]
  (let [parse-re #"(\S+).+?[\[](\S+).+?\"(.+?)\""
        parsed (re-find parse-re line)]
    {:entry (parsed 0)
     :ip (parsed 1)
     :date (parsed 2)
     :req (parsed 3)}))

(defn parse-log
  "parse log file fname"
  [fname]
  (let [lines (read-log fname)]
    (mapv parse-line lines)))

(comment
  (parse-log "testdata/small.log"))
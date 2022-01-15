(ns agip.logparse
  (:require ["fs" :as fs]
            [cljs.reader :as reader]
            [clojure.string :as s]
            [agip.dateparser :as dp]))

(defonce config-file "config.edn")
(defonce geo-api-key (atom nil))

(defn read-config
  "read the config file and set api key"
  []
  (reset! geo-api-key
        (-> (fs/readFileSync config-file)
            (.toString)
            (reader/read-string)
            (:API-KEY))))

(comment (read-config)
         (println @geo-api-key))

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
     :date (dp/datestr->zulu (parsed 2))
     :req (parsed 3)}))

(defn parse-log
  "parse log file fname"
  [fname]
  (let [lines (read-log fname)]
    (mapv parse-line lines)))

(comment
  (parse-log "testdata/small.log"))
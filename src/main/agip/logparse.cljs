(ns agip.logparse
  (:require #_["fs" :as fs]
            #_[cljs.reader :as reader]
            [clojure.string :as s]
            [agip.dateparser :as dp]
            [agip.utils :as u]))



(defn read-log
  "read log file"
  [fname]
  (-> (u/slurp fname)
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

(defn log-reducer
  "transform raw log lines into maps of form {ip {parsed log data map}}"
  [acc log-line]
  (let [ip (:ip log-line)
        stripped-ll (dissoc log-line :ip)]
    (if-let [events (get-in acc [ip :events])]
      ;; this ip is already in reduced log, so conj new event
      (assoc acc ip {:events (conj events stripped-ll)})
      ;; this ip is not yet in reduced log, so make a new entry
      (assoc acc ip {:events [stripped-ll]}))))

(defn reduce-log
  "convert logfile from ip to vector of events"
  [logfname]
  (reduce log-reducer {} (parse-log logfname)))

#_:clj-kondo/ignore
(defn -main
  [& args]
  (u/init-app)
  )

(comment
  u/config
  (parse-log "testdata/small.log")
  (reduce-log "testdata/small.log")
  )
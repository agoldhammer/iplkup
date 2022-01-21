(ns agip.logparse
  (:require [process :as pr]
            [cljs.core.async :as a]
            [clojure.string :as s]
            [agip.dateparser :as dp]
            [agip.output :as out]
            [agip.utils :as u]
            [agip.rdns :as rdns]
            [agip.ipgeo :as ipg]))

(pr/on "uncaughtException", (fn [err origin]
                              (println "Uncaught Exception" err origin)))

(def done-chan (a/chan))

(defn- read-log
  "read log file"
  [fname]
  (-> (u/slurp fname)
      (s/split-lines)))

(defn- parse-line
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
  (try
    (let [lines (read-log fname)]
      (if (nil? lines)
        (throw (js/Error. "log is empty"))
        (mapv parse-line lines)))
    (catch :default e (throw (js/Error. "no input")))))

;; the reduced log is a map with ips as keys
;; for each ip, the value is a vector of events
;; supplemented eventually by two additional maps
;; the first of these has key :geodata with value provided by geo lookup
;; the second has key :hostname with value provided by reverse dns
;; {ip-as-string {:events [vec of events]
;;                :geodta {geomap} :hostname hostname}}
;; each event is a map {:date date :req request :entry full-log-line}

(defn- log-reducer
  "transform raw log lines into maps of form {ip {parsed log data map}}"
  [acc log-line]
  (let [ip (:ip log-line)
        stripped-ll (dissoc log-line :ip)]
    (if-let [events (get-in acc [ip :events])]
      ;; this ip is already in reduced log, so conj new event
      (assoc acc ip {:events (conj events stripped-ll)})
      ;; this ip is not yet in reduced log, so make a new entry
      (assoc acc ip {:events [stripped-ll]}))))

(defn- reduce-log
  "convert logfile from ip to vector of events"
  [logfname]
  (reduce log-reducer {} (parse-log logfname)))

(defn- combine-geo
  [item log]
  (let [geodata (:geodata item)
        ip (:ip geodata)
        stripped-geo (dissoc geodata :ip)]
    (assoc-in log [ip :geodata] stripped-geo)))

(defn- augment-log-geo
  "augment reduced log with geodata, result in channel"
  [reduced-log]
  (let [ips (keys reduced-log)
        geochan (ipg/ips->geochan ips)
        outchan (a/chan)]
    (a/go-loop [item (a/<! geochan)
                augmented-log reduced-log]
      (if (nil? item)
        (a/>! outchan augmented-log)
        (recur (a/<! geochan)
               (combine-geo item augmented-log))))
    outchan))

(defn- reduce-with-hostnames
  "fold zipmap of [ip {:hostname hostanme}] into log"
  [log zipmapped]
  (out/pp-log (reduce-kv #(assoc-in  %1 [%2 :hostname]
                                     (:hostname %3)) log zipmapped))
  (a/put! done-chan :done))

(defn- augment-log-hostnames
  "augment reduced log with hostnames, result in channel"
  [log]
  (let [ips (keys log)]
    (rdns/ips->output-chan ips))
  (a/go (a/take! rdns/output-chan #(reduce-with-hostnames log %)))
  (a/take! done-chan #(println "lookups done")))

(defn- augment-log
  "read log file, reduce log, and augment with both geodata and hostnames"
  [fname]
  (let [reduced-log (reduce-log fname)
        geochan (augment-log-geo reduced-log)]
    (a/take! geochan augment-log-hostnames)))

(defn -main
  [& args]
  (pr/on "exit" (fn [code] (js/console.log "exiting" code)))
  (println "logrdr reading log file")
  ;; usage: if file name specified on command line, read it
  ;; otherwise read from stdin
  (let [fname (cond
                (nil? (first args)) "/dev/stdin"
                :else (first args))]
    (try
      (u/init-app)
      (augment-log fname)
      (a/take! done-chan #(println "logrdr done"))
      (catch js/Error e (println "Error:" (ex-message e))))))

(comment
  (-main "testdata/small.log"))



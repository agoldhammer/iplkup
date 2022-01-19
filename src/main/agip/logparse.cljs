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

;; the reduced log is a map with ips as keys
;; for each ip, the value is a vector of events
;; supplemented eventually by two additional maps
;; the first of these has key :geodata with value provided by geo lookup
;; the second has key :hostname with value provided by reverse dns
;; {ip-as-string {:events [vec of events]
;;                :geodta {geomap} :hostname hostname}}
;; each event is a map {:date date :req request :entry full-log-line}

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

(defn combine-geo
  [item log]
  (let [geodata (:geodata item)
        ip (:ip geodata)
        stripped-geo (dissoc geodata :ip)]
    #_(println "combinegeo: " geodata)
    #_(println "combineip: " ip)
    #_(println "--")
    (assoc-in log [ip :geodata] stripped-geo)))

(defn augment-log-geo
  "augment reduced log with geodata, result in channel"
  [reduced-log]
  (let [ips (keys reduced-log)
        geochan (ipg/ips->geochan ips)
        outchan (a/chan)]
    #_(a/go-loop [item (a/<! geochan)]
        (when item
          (println "a-l-g: " item)
          #_(tap> {:aug-smpl item})
          (recur (a/<! geochan))))
    (a/go-loop [item (a/<! geochan)
                augmented-log reduced-log]
      #_(tap> {:aug-item item})
      #_(println {:aug-item item})
      (if (nil? item)
        (a/put! outchan augmented-log #(println "alg done"))
        (recur (a/<! geochan)
               (combine-geo item augmented-log))))
    outchan))

(defn hostlookups
  "lookup hostnames from ips in file"
  [fname]
  (let [ips (s/split-lines (u/slurp fname))]
    ips))

#_:clj-kondo/ignore
(defn -main
  [& args]
  (pr/on "exit" (fn [code] (js/console.log "exiting" code)))
  (u/init-app)
  #_(u/reset-debug)
  ;;; for combining geodata into reduced log
  #_(a/take! (augment-log-geo (reduce-log (first args))) out/pp-log)

  ;;; for testing hostlookups
  #_(rdns/process-ips (hostlookups (first args)))
  (rdns/ips->ouput-chan (hostlookups (first args)))
  (a/go (do (doseq [item (a/<! rdns/output-chan)]
              (println "**" item))
            (a/put! rdns/done-chan :done)))
  (a/take! rdns/done-chan #((do (println "lookups done")
                                (pr/exit 0))))
  #_(println @u/debug-a)
  #_(js/setTimeout #(pr/exit 0) 4500)
  #_(pr/exit 0))

(comment
  (hostlookups "ips.txt")
  u/config
  (u/reset-debug)
  (parse-log "testdata/small.log")
  (reduce-log "testdata/small.log")
  (def small-reduced (reduce-log "testdata/small.log"))
  (out/pp-log small-reduced)
  (def res (augment-log-geo (reduce-log "testdata/small.log")))
  res
  (a/take! (augment-log-geo (reduce-log "testdata/small.log")) out/pp-log)
  @u/debug-a
  (out/pp-log (reduce-log "testdata/newer.log"))
  )
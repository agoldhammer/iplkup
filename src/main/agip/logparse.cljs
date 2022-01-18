(ns agip.logparse
  (:require #_["fs" :as fs]
            #_[cljs.reader :as reader]
            [process :as pr]
            [cljs.core.async :as a]
            [clojure.string :as s]
            [agip.dateparser :as dp]
            [agip.output :as out]
            [agip.utils :as u]
            [agip.ipgeo :as ipg]))



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
    (println "combinegeo: " geodata)
    (println "combineip: " ip)
    (println "--")
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
      (println {:aug-item item})
      (if (nil? item)
        (do
          (println "exiting alg")
          (a/put! outchan augmented-log #(println "alg done")))
        (recur (a/<! geochan)
               (combine-geo item augmented-log))))
    outchan))

#_:clj-kondo/ignore
(defn -main
  [& args]
  (u/init-app)
  (u/reset-debug)
  (a/take! (augment-log-geo (reduce-log "testdata/small.log")) out/pp-log)
  #_(println @u/debug-a)
  (js/setTimeout #(pr/exit 0) 4500)
  )

(comment
  u/config
  (u/reset-debug)
  (parse-log "testdata/small.log")
  (reduce-log "testdata/small.log")
  (def small-reduced (reduce-log "testdata/small.log"))
  (out/pp-log small-reduced)
  (def res (augment-log-geo (reduce-log "testdata/small.log")))
  res
  (out/pp-log (augment-log-geo (reduce-log "testdata/small.log")))
  @u/debug-a
  (out/pp-log (reduce-log "testdata/newer.log"))

  (def geo {:geodata {:country_code2 "US", :ip "54.245.183.198", :city "Durham", :longitude "-78.86284", :zipcode "27709", :country_name "United States", :country_code3 "USA", :latitude "35.90841", :state_prov "North Carolina", :district "Research Triangle Park"}}
)
  (println (get-in (combine-geo geo small-reduced) ["54.245.183.198" :geodata]))

  )
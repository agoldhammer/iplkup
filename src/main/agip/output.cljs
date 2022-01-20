(ns agip.output)

(defn pp-log-entry
  "pretty print the reduced log entry
   destructured as ip and data"
  [ip data]
  (let [{:keys [events hostname geodata]} data]
    (println (str "ip: " ip))
    (println "hostname:" hostname)
    (println "geodata: " geodata)
    (doseq [event events]
      (println "  **")
      (println (str "  ...date/time: " (:date event)))
      (println (str "  ...req: " (:req event)))
      (println (str "  ...entry: " (:entry event))))
    (println (apply str (repeat 80 "-")))))

(defn pp-log
  "pretty print the log"
  [reduced-log]
  ;; destructure each log entry by key (ip) 
  ;; and value (events, geodata, hostname)
  (doseq [[ip data] reduced-log]
    (pp-log-entry ip data)))
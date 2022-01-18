(ns agip.output)

(defn pp-reduced-log-entry
  "pretty print the reduced log entry
   destructured as ip and data"
  [ip data]
  (let [{:keys [events site-data geodata]} data]
    (println (str "ip: " ip))
    (println "site-data" site-data)
    (println "geodata: " geodata)
    (doseq [event events]
      (println "  **")
      (println (str "  ...date/time: " (:date event)))
<<<<<<< HEAD
      (println (str "  ...req: " (:req event)))
      (println (str "  ...entry: " (:entry event))))
=======
      (println (str "  ...entry: " (:entry event)))
      (println (str "  ...req: " (:req event))))
>>>>>>> dd913bbb5e878df3f9fe63e1d2fa4988d3fe7095
    (println "---")))

(defn pp-reduced-log
  "pretty print the reduced log"
  [reduced-log]
  ;; destructure each log entry by key (ip) 
  ;; and value (events, geodata, hostname)
  (doseq [[ip data] reduced-log]
    (pp-reduced-log-entry ip data)))
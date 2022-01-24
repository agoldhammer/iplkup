(ns agip.output
  (:require ["cli-color" :as col]))

(defn- geodata->strings
  "convert site data to a vector of strings"
  [data]
  (let [{:keys [country_code2 country_name city state_prov
                district latitude longitude]} data
        line1 (col/cyan (str "  " country_name " (" country_code2 ")"))
        line2 (col/cyan (str "  " (when (not= "" city) (str city ", "))
                             state_prov
                             (when (not= "" district)
                               (col/magenta (str " (district: " district ")")))))
        line3 (col/cyan (str "  lat-lon: " latitude ", " longitude))]
    [line1 line2 line3]))

(defn- pp-log-entry
  "pretty print the reduced log entry
   destructured as ip and data"
  [ip data]
  (let [{:keys [events hostname geodata]} data
        geo-lines (geodata->strings geodata)]
    (println (col/green (str "ip: " ip)))
    (println (col/yellow (str "hostname: " hostname)))
    (println (col/red "geodata:"))
    (doseq [line geo-lines]
      (println line))
    (doseq [event events]
      (println "  **")
      (println (str "  ...date/time: " (:date event)))
      (println (str "  ...req: " (:req event)))
      ;; retain only 75 chars of long events
      (let [full-entry (:entry event)
            cnt (count full-entry)
            trunc-entry (subs full-entry 0 (min cnt 90))]
        (println (str "  ...entry: " trunc-entry))))
    (println (apply str (repeat 80 "-")))))

(defn pp-log
  "pretty print the log"
  [reduced-log]
  ;; destructure each log entry by key (ip) 
  ;; and value (events, geodata, hostname)
  (doseq [[ip data] reduced-log]
    (pp-log-entry ip data)))
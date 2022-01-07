(ns agip.rdns
  (:require ["fs" :as fs]
            ["dns" :as dns]
            [process :as p]
            [clojure.string :as s]))

(p/on "uncaughtException", (fn [err origin]
                             (println "Uncaught Exception" err origin)))

(defn slurp [file]
  (-> (fs/readFileSync file)
      (.toString)))


(defn rev-dns
  "do reverse dns lookup on vec of ips"
  [ips]
  (mapv #(dns/promises.reverse % "CNAME") ips))

(def ips (s/split-lines (slurp "ips.txt")))

#_(def proms (rev-dns (take 6 ips)))

(defn proc-proms
  "print a sequence of dns reverse lookups of vec of ips"
  [ips]
  (doseq [prom (rev-dns ips)]
    #_(println prom)
    (-> prom
        (.then #(js/console.log %))
        (.catch #(js/console.log "lkup err" %)))))

#_(.then (first proms) #(println (js->clj %)))

(defn -main
  [& args]
  (println "main" args))

(comment
  (js/console.log "err" 2)
  (def prom (dns/promises.reverse "192.74.137.6" "CNAME"))
  (.then prom #(println (js->clj %))) println
  (.then (dns/promises.reverse "51.79.29.48" println))
  (proc-proms (take 5 ips))
  )


(comment
  (println "hi" 5)
  (def s (slurp "ips.txt"))
  (s/split-lines s)
  (dns/lookup "example.org"
              (fn [err add fam]
                (println err add fam))))






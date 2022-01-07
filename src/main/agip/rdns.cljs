(ns agip.rdns
  (:require ["fs" :as fs]
            ["dns" :as dns]
            [clojure.string :as s]))

(defn dumb []
  (println "ans" (+ 2 2)))

(dumb)
(println "hi" 5)

(defn slurp [file]
  (-> (fs/readFileSync file)
      (.toString)))

(def s (slurp "ips.txt"))
(s/split-lines s)
(dns/lookup "example.org"
            (fn [err add fam]
              (println err add fam)))

(def dns-promises dns/promises)

(def prom (dns-promises.reverse "192.74.137.6" "CNAME"))
(.then prom #(println (js->clj %)))

(defn rev-dns
  "do reverse dns lookup on vec of ips"
  [ips]
  (mapv dns-promises.reverse ips))

(def ips (s/split-lines (slurp "ips.txt")))
(take 3 ips)

(def proms (rev-dns (take 15 ips)))
(take 3 proms)
(doseq [prom (take 15 proms)]
  (.then prom #(println (js->clj %))))

(first proms)
(count proms)

(.then (first proms) #(println (js->clj %)))






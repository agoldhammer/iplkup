(ns agip.rdns
  (:require ["fs" :as fs]
            ["dns" :as dns]
            [process :as p]
          [clojure.string :as s]))

(p/on "uncaughtException", (fn[err origin]
                                   (println "Uncaught Exception" err origin)))

(defn slurp [file]
  (-> (fs/readFileSync file)
      (.toString)))

#_(def dns-promises dns/promises)

(defn rev-dns
  "do reverse dns lookup on vec of ips"
  [ips]
(mapv dns/promises.reverse ips))


#_(defn rev-dns2
  "do reverse dns lookup on vec of ips"
  [ips]
  (mapv dns-promises.reverse ips))

(def ips (s/split-lines (slurp "ips.txt")))
(take 3 ips)
(count ips)

(def proms (rev-dns (take 15 ips)))
(take 3 proms)
(doseq [prom (take 15 proms)]
  (.then prom #(println (js->clj %))))

(first proms)
(count proms)

(.then (first proms) #(println (js->clj %)))

(defn -main
  [& args]
  (println "main" args))

(comment
(def prom (dns/promises.reverse "192.74.137.6" "CNAME"))
  (.then prom #(println (js->clj %))))


(comment 
         (println "hi" 5)
         (def s (slurp "ips.txt"))
         (s/split-lines s)
         (dns/lookup "example.org"
                     (fn [err add fam]
                       (println err add fam))))






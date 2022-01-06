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

(def prom (dns-promises.reverse "192.74.137.5" "A"))
(.then prom #(println (js->clj %)))



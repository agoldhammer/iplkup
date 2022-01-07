(ns agip.rdns
  (:require ["fs" :as fs]
            ["dns" :as dns]
            [process :as p]
            [cljs.core.async :as a]
            #_[cljs.core.async.interop :refer-macros [>!]]
            [clojure.string :as s]))

(p/on "uncaughtException", (fn [err origin]
                             (println "Uncaught Exception" err origin)))

(defn slurp [file]
  (-> (fs/readFileSync file)
      (.toString)))


#_(defn rev-dns
  "do reverse dns lookup on vec of ips"
  [ips]
  (mapv #(dns/promises.reverse % "CNAME") ips))

(def ips (s/split-lines (slurp "ips.txt")))

(defn process-ips
  "create outch and put seq of dns reverse lookups of vec of ips on outch
   return the outch"
  [ips]
  (let [out-ch (a/chan 256)]
    (a/go
      (doseq [ip ips]
        (println ip)
        (a/>! out-ch {:ip ip :promise (dns/promises.reverse ip "CNAME")})))
    out-ch))

(defn resolve-hostname
  "resolve hostname from promise"
  [prom]
  (-> prom
      (.then println #_#(js->clj %) )
      (.catch #(identity "N/A"))))

(defn -main
  [& args]
  (println "main" args))

(comment
  (js/console.log "err" 2)
  (-> (js/Promise.reject (js/Error "fail"))
      (.then #(println (.-Error %)))
      (.catch #(js/console.log %))
      #_(.catch #(js/console.log (ex-cause %)))
      (.finally #(println "done")))
  (def prom (dns/promises.reverse "192.74.137.6" "CNAME"))
  (.then prom #(println (js->clj %)))
  (.then (dns/promises.reverse "51.79.29.48" println))
  (count ips)
  (let [c (process-ips (take 5 ips))]
    (a/go-loop [item (a/<! c)
                acc []]
      (if item
        (do
          (println "loop check" item)
          (let [{:keys [ip promise]} item
                host (resolve-hostname promise)
                _ (println "host return" host)]
            (recur (a/<! c) (into acc {:ip ip :host host}))))
        (println acc))))
  )

(comment
  (def p (dns/promises.reverse "34.86.35.10" "CNAME"))
  (.then p #(println (js->clj %)))
  (resolve-hostname p)
  (println "hi" 5)
  (def s (slurp "ips.txt"))
  (s/split-lines s)
  (dns/lookup "example.org"
              (fn [err add fam]
              (println err add fam))))






(ns agip.rdns
  (:require ["fs" :as fs]
            ["dns" :as dns]
            [process :as p]
            [clojure.core.async :as a]
            #_[cljs.core.async.interop :refer-macros [>!]]
            [clojure.string :as s]))

(p/on "uncaughtException", (fn [err origin]
                             (println "Uncaught Exception" err origin)))

(def exit-chan (a/chan 1))

(defn slurp [file]
  (-> (fs/readFileSync file)
      (.toString)))


#_(defn rev-dns
    "do reverse dns lookup on vec of ips"
    [ips]
    (mapv #(dns/promises.reverse % "CNAME") ips))

(defn ip->host
  "reverse lkup ip asynchronously
   return map {:ip ip :promise prom}
   where prom should resolve to hostname"
  [ip]
  (println "processing ip:" ip)
  (dns/promises.reverse ip "CNAME"))

(defn async-process-ip
  "pipeline function to revese lkup ip"
  [ip result-ch]
  (a/go [ip result-ch]
        (println "async-process-ip processing ip " ip)
        (a/>! result-ch {:ip ip :promise (ip->host ip)})
        (a/close! result-ch)))

(defn pipe-ips
  "create outch and put seq of dns reverse lookups of vec of ips on outch
   return the outch"
  [ips]
  (let [out-ch (a/chan 256)]
    (a/pipeline-async 8 out-ch async-process-ip (a/to-chan! ips))
    out-ch))

(defn resolve-item
  "resolve hostname from promise"
  [item]
  (a/go
    (let [{:keys [ip promise]} item]
      (println "resolve-item" item)
      (println "resolve-item" ip promise)
      (-> promise
          (.then js/console.log)
          (.catch js/console.log)
          (.finally #(js/console.log "fin" ip))))))

(defn process-ips
  "process a vector of ips"
  [ips]
  (let [out-ch (pipe-ips ips)
        n (atom (count ips))]
    (a/go-loop [item (a/<! out-ch)]
      (println "process-ips loop" @n)
      (if item
        (do
          (resolve-item item)
          (swap! n dec)
          (recur (a/<! out-ch)))
        (a/>! exit-chan :done)))
    ;; spin until exit signal
    #_(while (nil? (a/poll! exit-chan))
      (reduce + (range 1000)))
    #_(while (not= @n 0)
      (println "wait count" @n)
      (js/setTimeout #(println "waiting" @n) 30000))))

(defn process-file
 "process a file of ips"
 [fname]
  (let [ips (s/split-lines (slurp fname))]
    (println "processing file" fname "with" (count ips) "ips")
    (process-ips ips)))

(defn -main
  [& args]
  (process-file "ips.txt")
  (println "main" args))

(comment
  (process-ips ["34.86.35.10" "52.41.81.117"])
  (process-ips (take 4 (s/split-lines (slurp "ips.txt")))))

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
  #_(let [c (process-ips (take 5 ips))]
      (a/go-loop [item (a/<! c)
                  acc []]
        (if item
          (do
            (println "loop check" item)
            (let [{:keys [ip promise]} item
                  host (resolve-item promise)
                  _ (println "host return" host)]
              (recur (a/<! c) (into acc {:ip ip :host host}))))
          (println acc)))))

(comment
  (def p (dns/promises.reverse "34.86.35.10" "CNAME"))
  (.then p #(println (js->clj %)))
  (println "hi" 5)
  (def s (slurp "ips.txt"))
  (s/split-lines s)
  (dns/lookup "example.org"
              (fn [err add fam]
                (println err add fam))))

(comment
  (let
   [ips (s/split-lines (slurp "ips.txt"))
    c (a/chan 256)]
    (count ips)
    (ip->host "34.86.35.10")
    (println "wait 1")
    (js/setTimeout #(a/go (println (a/<! c))) 1000)) )






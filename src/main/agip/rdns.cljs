(ns agip.rdns
  (:require ["fs" :as fs]
            ["dns" :as dns]
            [process :as pr]
            [clojure.core.async :as a]
            #_[cljs.core.async.interop :refer-macros [<p!]]
            [clojure.string :as s]))

(pr/on "uncaughtException", (fn [err origin]
                             (println "Uncaught Exception" err origin)))

#_(def exit-chan (a/chan 1))
(enable-console-print!)

(defn slurp [file]
  (-> (fs/readFileSync file)
      (.toString)))


#_(defn rev-dns
    "do reverse dns lookup on vec of ips"
    [ips]
    (mapv #(dns/promises.reverse % "CNAME") ips))

(defn ip->host
  "reverse lkup ip"
  [ip]
  (println "processing ip:" ip)
  (dns/promises.reverse ip "CNAME"))

(comment
  (dns/reverse "34.86.35.10" println)
  (let [p (ip->host "172.182.1.25")]
    (-> p
        (.then println println)
        (.catch println println)
        (.finally #(println "finis"))))
  ;;;
  #_(a/take! (a/go
             (try
               (<p! (ip->host "172.182.1.25"))
               (catch js/Error err (js/console.log (ex-cause err))))) println)

  ;;;
  (comment
    (def p (js/Promise.all (mapv ip->host ["34.86.35.10" #_"172.182.1.25"])))

    (-> p
        (.then println)
        (.catch println))
    )
  (dns/reverse "34.86.35.10" (fn [err hns]
                               (if err
                                 (println err)
                                 (println hns))))
  (dns/reverse "172.182.1.25" (fn [err hns]
                                (if err
                                  (println "error")
                                  (println hns)))))

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

#_(defn resolve-item
  "resolve hostname from promise"
  [item]
  (a/go
    (let [{:keys [ip promise]} item]
      (println "resolve-item" item)
      (println "resolve-item" ip promise)
      #_(-> promise
            (.then js/console.log)
            (.catch js/console.log)
            (.finally #(js/console.log "fin" ip)))
      (try
        (a/take! (<p! promise) println)
        (catch js/Error err (js/console.log (ex-cause err)))))))

(defn process-ips
  "process a vector of ips"
  [ips]
  (let [out-ch (pipe-ips ips)]
    (a/go-loop [item (a/<! out-ch)
                acc []]
      (println "process-ips loop")
      (if item
        (recur (a/<! out-ch) (into acc item))
        #_(println "accum" acc)
        (let [ips (mapv (comp first rest) (filter #(= :ip (get % 0)) acc))
              proms (mapv (comp first rest) (filter #(= :promise (get % 0)) acc))
              settled (js/Promise.allSettled proms)]
          (println "in process-ips: nproms" (count proms))
          (println "ips" ips)
          (->
           settled
           (.then #(js/console.log "output" %))
           (.catch #(js/console.log "error"))
           (.finally #(println :finis))))))))

(defn process-file
  "process a file of ips"
  [fname]
  (let [ips (s/split-lines (slurp fname))]
    (println "processing file" fname "with" (count ips) "ips")
    (process-ips ips)))

(defn -main
  [& args]
  (pr/on "exit" (fn [code] (js/console.log "exiting" code)))
  (println "Welcome" args)
  (process-file "ips.txt")
  (js/setTimeout #(pr/exit 0) 1000)
  #_(println "main" args))

(comment
  (do
    (process-ips ["34.86.35.10" "52.41.81.117"])
    (pr/on "exit" (fn [code] (js/console.log "exiting" code))))
  (-main "hi")
(process-ips (take 5 (s/split-lines (slurp "ips.txt"))))

  )

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
  )

(comment
  (def p (dns/promises.reverse "34.86.35.10" "CNAME"))
  (.then p #(println (js->clj %)))
  (println "hi" 5)
  (def s (slurp "ips.txt"))
  (s/split-lines s)
  (dns/lookup "example.org"
              (fn [err add fam]
                (println err add fam)))
  (js/setTimeout #(println "hi 2") 2000))


(comment
  (let
   [ips (s/split-lines (slurp "ips.txt"))
    c (a/chan 256)]
    (count ips)
    (ip->host "34.86.35.10")
    (println "wait 1")
    (js/setTimeout #(a/go (println (a/<! c))) 1000)))






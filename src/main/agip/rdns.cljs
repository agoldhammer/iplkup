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

;; from David Nolen gist
(defn timeout [ms]
  (let [c (a/chan)]
    (js/setTimeout (fn [] (a/close! c)) ms)
    c))

(comment
  (a/go
    (a/<! (timeout 1000))
    (println "Hello")
    (a/<! (timeout 1000))
    (println "async")
    (a/<! (timeout 1000))
    (println "world!"))
  )

(defn slurp [file]
  (-> (fs/readFileSync file)
      (.toString)))


(defn ip->host
  "reverse lkup ip"
  [ip]
  #_(println "processing ip:" ip)
  (dns/promises.reverse ip "CNAME"))

(comment
  (dns/reverse "34.86.35.10" println)
  (let [p (ip->host "172.182.1.25")]
    (-> p
        (.then println println)
        (.catch println println)
        (.finally #(println "finis"))))
  
  )

(defn async-process-ip
  "pipeline function to revese lkup ip"
  [ip result-ch]
  (a/go [ip result-ch]
        #_(println "async-process-ip processing ip " ip)
        (a/>! result-ch {:ip ip :promise (ip->host ip)})
        (a/close! result-ch)))

(defn pipe-ips
  "create outch and put seq of dns reverse lookups of vec of ips on outch
   return the outch"
  [ips]
  (let [out-ch (a/chan 256)]
    (a/pipeline-async 8 out-ch async-process-ip (a/to-chan! ips))
    out-ch))

(defn make-host-channel
  "create and return channel to receive outcome of host lookups"
  []
  (let [host-chan (a/chan 1)]
    (a/go-loop [item (a/<! host-chan)]
      (when item
        (let [[ips settled] item]
          (println  "host-chan count" (count ips))
          #_(println settled)
          (->
           settled
           (.then #(js/console.log "output" %))
           (.catch #(js/console.log "error"))
           (.finally #(println :finis))))
        
        (recur (a/<! host-chan))))
    host-chan))


(defn process-ips
  "process a vector of ips"
  [ips]
  (let [out-ch (pipe-ips ips)
        host-chan (make-host-channel)]
    (a/go-loop [item (a/<! out-ch)
                acc []]
      #_(println "process-ips loop")
      (if item
        (recur (a/<! out-ch) (into acc item))
        (let [ips (mapv (comp first rest) (filter #(= :ip (get % 0)) acc))
              proms (mapv (comp first rest) (filter #(= :promise (get % 0)) acc))
              settled (js/Promise.allSettled proms)]
          #_(println "in process-ips: nproms" (count proms))
          (a/>! host-chan [ips settled]))))))

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
  (js/setTimeout #(pr/exit 0) 2000)
  #_(println "main" args))

(comment
  (process-ips ["34.86.35.10" "52.41.81.117"])
  (-main "hi")
  (process-ips (take 5 (s/split-lines (slurp "ips.txt"))))
  (def ps (js/Promise.resolve (#(js/setTimeout (fn [] {:ans 42}))) 1000))
  (.then ps println)
  )






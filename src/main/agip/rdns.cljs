(ns agip.rdns
  (:require [clojure.core.async :as a]
            ["dns" :as dns]))

(def output-chan (a/chan 256))
(def done-chan (a/chan))

(defn- async-process-ip
  "pipeline function to revese lkup ip"
  [ip result-ch]
  (a/go [ip result-ch]
        (a/>! result-ch {:ip ip :promise (dns/promises.reverse ip "CNAME")})
        (a/close! result-ch)))

(defn- pipe-ips
  "create outch and put seq of dns reverse lookups of vec of ips on outch
   return the outch"
  [ips]
  (let [out-ch (a/chan 256)]
    (a/pipeline-async 8 out-ch async-process-ip (a/to-chan! ips))
    out-ch))

(defn- zip-ips-to-hostnames
  "zipmap ips to resolved hostnames"
  [ips hostresolutions]
  ;; the resolved hostname is a #js array
  ;; must convert to cljs and take the first element
  (let [fix-hostname (fn [hostres]
                       (if (= hostres.status "fulfilled")
                         {:hostname (first (js->clj hostres.value))}
                         {:hostname "N/A"}))
        zipped-ips-hosts (zipmap ips
                                 (into [] (map fix-hostname hostresolutions)))]
    (a/put! output-chan zipped-ips-hosts #(println "hostnames resolved"))))

(defn- make-host-channel
  "create and return channel to receive outcome of host lookups"
  []
  (let [host-chan (a/chan 1)]
    (a/go-loop [item (a/<! host-chan)]
      (when item
        (let [[ips settled] item]
          #_(println  "host-chan count" (count ips))
          (->
           settled
           (.then #(zip-ips-to-hostnames ips %))
           (.catch #(js/console.log "error"))
           #_(.finally #(println :finis))))

        (recur (a/<! host-chan))))
    host-chan))

(defn ips->output-chan
  "resolve vector of ips and put result on host-chan"
  [ips]
  (let [ip-ch (pipe-ips ips)
        host-chan (make-host-channel)]
    ;; accumulate [:ip ip :promise prom] items from the out-ch of the ip pipeline
    ;; resolve the promises and restructure as [[ip1 ip2 ...] ["hostname1" "hostname2" ...]]
    ;; send this to host-chan
    (dns/setServers #js ["8.8.8.8"])
    (a/go-loop [item (a/<! ip-ch)
                acc []]
      (if item
        (recur (a/<! ip-ch) (into acc item))
        (let [ips (mapv (comp first rest) (filter #(= :ip (get % 0)) acc))
              proms (mapv (comp first rest) (filter #(= :promise (get % 0)) acc))
              settled (js/Promise.allSettled proms)]
          (a/>! host-chan [ips settled]))))))







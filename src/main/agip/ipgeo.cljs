(ns agip.ipgeo
  (:require #_["fs" :as fs]
            ["xmlhttprequest" :refer [XMLHttpRequest]]
            [agip.utils :as u]
            #_[cljs.nodejs :as nodejs]
            [cljs-http.client :as http]
            [cljs.core.async :as a]))

;; for this hack, needed to make cljs-http work properly
;; see http://www.jimlynchcodes.com/blog/solved-xmlhttprequest-is-not-defined-with-clojurescript-coreasync

#_(set! js/XMLHttpRequest (nodejs/require "xhr2"))
;; this eliminates the annoying message from xhr2, which is no longer required
(set! js/XMLHttpRequest XMLHttpRequest)

(defn get-site-data
  "fetch site data for ip, place in suppliedc hannel"
  [ip outch]
  {:pre [(not (nil? (:geo-api-key u/config)))]}
  (let [base-url "https://api.ipgeolocation.io/ipgeo"
        url (str base-url "?apiKey=" (:geo-api-key u/config) "&ip=" ip "&fields=geo")]
    (http/get url {:channel outch})))

(defn resp->geodata
  "transform ipgeo response as needed"
  [resp]
  #_(tap> (str "r->g" resp))
  (if (= (:status resp) 200)
    {:geodata (:body resp)}
    {:geodata "N/A"}))

(defn ips->raw-site-data-chan
  "pipeline a vector of ips to a channel containing a vector of site-data;
   return site-data-chan"
  [ips]
  (let [raw-site-data-chan (a/chan 1024)]
    (a/pipeline-async 1 raw-site-data-chan get-site-data (a/to-chan! ips))
    raw-site-data-chan))

(defn raw-site-data-chan->site-data-chan
  "transform raw response from ipgeo lookup to clean response chan
   using pipeline with transducer, return site-data-chan"
  [raw-site-data-chan]
  (let [site-data-chan (a/chan 1024)]
    (a/go-loop [raw-response (a/<! raw-site-data-chan)]
               (when raw-response
                 (a/>! site-data-chan (resp->geodata raw-response))
                 (recur (a/<! raw-site-data-chan))))
    site-data-chan))

(comment
  (def raw-site-data-chan (ips->raw-site-data-chan ["8.8.8.8" "9.8.8.8"]))
  #_(a/take! raw-site-data-chan println)
  (def site-data-chan (raw-site-data-chan->site-data-chan raw-site-data-chan))
  (a/take! site-data-chan println)
)


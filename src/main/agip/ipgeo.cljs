(ns agip.ipgeo
  (:require #_["https" :as https]
            ["fs" :as fs]
            ["xmlhttprequest" :refer [XMLHttpRequest]]
            #_[cljs.nodejs :as nodejs]
            [cljs-http.client :as http]
            [cljs.reader :as reader]
            [cljs.core.async :as a]))

;; for this hack, needed to make cljs-http work properly
;; see http://www.jimlynchcodes.com/blog/solved-xmlhttprequest-is-not-defined-with-clojurescript-coreasync

#_(set! js/XMLHttpRequest (nodejs/require "xhr2"))
;; this eliminates the annoying message from xhr2, which is no longer required
(set! js/XMLHttpRequest XMLHttpRequest)

;; see https://quanttype.net/posts/2018-10-18-how-i-use-tap.html
(def debug-a (atom nil))
(add-tap #(reset! debug-a %))

(def base-url "https://api.ipgeolocation.io/ipgeo")

(defonce config-file "config.edn")
(defonce geo-api-key (atom nil))

(defn read-config
  "read the config file and set api key"
  []
  
  (reset! geo-api-key
          (-> (fs/readFileSync config-file)
              (.toString)
              (reader/read-string)
              (:API-KEY))))

(comment (read-config)
         (println @geo-api-key))

(defn get-site-data
  "fetch site data for ip, place in suppliedc hannel"
  [ip outch]
  {:pre [(not (nil? @geo-api-key))]}
  (let [url (str base-url "?apiKey=" @geo-api-key "&ip=" ip "&fields=geo")]
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
  (def mychan (a/chan 1))
  (get-site-data "8.8.8.8" mychan)
  (a/take! mychan #(println "done" (resp->geodata %)))
  (a/take! mychan #(tap> (resp->geodata %)))
  @debug-a
  ;;;
  (def raw-site-data-chan (ips->raw-site-data-chan ["8.8.8.8" "9.8.8.8"]))
  (a/take! raw-site-data-chan println)
  (def site-data-chan (raw-site-data-chan->site-data-chan raw-site-data-chan))
  (a/take! site-data-chan println)

)


(ns agip.ipgeo
  (:require #_["https" :as https]
            ["fs" :as fs]
            ["xmlhttprequest" :refer [XMLHttpRequest]]
            #_[cljs.nodejs :as nodejs]
            [cljs-http.client :as http]
            [cljs.reader :as reader]
            [cljs.core.async :as a]))

;; for this hack
;; see http://www.jimlynchcodes.com/blog/solved-xmlhttprequest-is-not-defined-with-clojurescript-coreasync

#_(set! js/XMLHttpRequest (nodejs/require "xhr2"))
;; this eliminates the annoying message from xhr2, which is no longer required
(set! js/XMLHttpRequest XMLHttpRequest)

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
  {:pre @geo-api-key}
  (let [url (str base-url "?apiKey=" @geo-api-key "&ip=" ip "&fields=geo")]
    (http/get url {:channel outch})))

(defn resp->geodata
  "transform ipgeo response as needed"
  [resp]
  (if (= (:status resp) 200)
    {:geodata (:body resp)}
    {:geodata "N/A"}))

(comment
  (def mychan (a/chan 1))
  (get-site-data "8.8.8.8" mychan)
  (a/take! mychan #(println "done" (resp->geodata %)))
)


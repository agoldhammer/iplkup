(ns agip.ipgeo
  (:require ["https" :as https]
            ["fs" :as fs]
            [cljs.reader :as reader]
            [clojure.core.async :as a]))

(def base-url "https://api.ipgeolocation.io/ipgeo")

(defonce config-file "config.edn")
(defonce geo-api-key (atom nil))

(defn read-config
  "read the config file and set api key"
  []
  (:pre (not (nil? @geo-api-key)))
  (reset! geo-api-key
          (-> (fs/readFileSync config-file)
              (.toString)
              (reader/read-string)
              (:API-KEY))))

(comment (read-config)
         (println @geo-api-key))

(defn response-fn
  [resp chan]
  (println "rf" resp)
  (a/put! chan resp))

(defn get-site-data
  "fetch site data for ip, place in pipeline"
  [ip result-chan]
  (let [url (str base-url "?apiKey=" @geo-api-key "&ip=" ip "&fields=geo")
        _ (println "the url is" url)]
    (https/get url (fn [resp] (response-fn resp result-chan)))
    #_(a/go
        (a/>! result-chan {:ip ip :site-data (https/get url)})
   ;; will need this for decoding
   ;; {:site-data (dissoc (ch/parse-string (:body resp) true) :ip)}
     ;; TODO removing below line produces only first log entry in seq, why??
        (a/close! result-chan))))

#_(ns agip.ipgeo
    (:require ["https" :as https]))

(comment 
  (let [mychan (a/chan 1)]
    (get-site-data "8.8.8.8" mychan)
    (a/go (println (a/<! mychan)))))
(ns agip.utils
  (:require ["fs" :as fs]
            [process]
            [clojure.edn :as edn]))

(def ^:private config nil)

(defn get-geo-api-key
  "return the geo api key set by config file"
  []
  (:geo-api-key config))

(defn get-nameservers
  "return the dns servers list"
  []
  (clj->js (:nameservers config)))

(defn slurp
  "read file into string"
  [fname]
  (-> (fs/readFileSync fname)
      (.toString)))

(defn- read-config
  "read the config file config.edn"
  []
  (let [home (.-HOME (.-env process))]
    (set! config (edn/read-string (slurp (str home "/.logrdr/config.edn"))))))

;; see https://quanttype.net/posts/2018-10-18-how-i-use-tap.html
(def debug-a (atom nil))

(defn- conj-tap
  [item]
  (swap! debug-a conj item))

(defn reset-debug
  "reset the debug tap"
  []
  (remove-tap conj-tap)
  (reset! debug-a [])
  (add-tap conj-tap))

(defn init-app
  "to initialize, read config and set config var"
  []
  ;; next 2 lines for debugging only
  (reset-debug)
  (read-config)
  (if (nil? (:geo-api-key config))
    (throw (js/Error. "No geo api key, check config.edn in ~/.logrdr"))
    :initialized))


(comment
  (.-HOME (.-env process))
  (init-app)
  (tap> "test")
  @debug-a
  (slurp "config.edn")
  (read-config)
  (init-app)
  config)
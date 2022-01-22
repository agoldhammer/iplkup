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
  (let [home (.. process -env -HOME)]
    (set! config (edn/read-string (slurp (str home "/.logrdr/config.edn"))))))

;; see https://quanttype.net/posts/2018-10-18-how-i-use-tap.html
#_(def debug-a (atom nil))

#_(defn- conj-tap
  [item]
  (swap! debug-a conj item))

#_(defn reset-debug
  "reset the debug tap"
  []
  (remove-tap conj-tap)
  (reset! debug-a [])
  (add-tap conj-tap))

(defn init-app
  "to initialize, read config and set config var"
  []
  ;; next 2 lines for debugging only
  #_(reset-debug)
  (read-config)
  (if (nil? (:geo-api-key config))
    (throw (js/Error. "No geo api key, check config.edn in ~/.logrdr"))
    :initialized))


(comment
  (.-HOME (.-env process))
  (.. process -env -HOME)
  (read-config)
  (init-app)
  config)
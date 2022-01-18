(ns agip.utils
  (:require ["fs" :as fs]
            [clojure.edn :as edn]))

(def config nil)

(defn slurp
  "read file into string"
  [fname]
  (-> (fs/readFileSync fname)
      (.toString)))

(defn- read-config
  "read the config file config.edn"
  []
  (set! config (edn/read-string (slurp "config.edn"))))

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
  :initialized)


(comment
  (init-app)
  (tap> "test")
  @debug-a
  (slurp "config.edn")
  (read-config)
  (init-app)
  config)
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

(defn init-app
  "to initialize, read config and set config var"
  []
  ;; next line for debugging only
  (add-tap #(reset! debug-a %))
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
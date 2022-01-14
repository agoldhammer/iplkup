(ns test.logparse-test
  (:require [cljs.test :refer [deftest testing is]]
            [agip.logparse :as lp]))

(deftest logparse-test
  (testing "parse log")
  (let [first-item (get (lp/parse-log "testdata/small.log") 0)]
    (is (= (:ip first-item) "54.245.183.198"))))

(comment
  (cljs.test/run-tests)
  )

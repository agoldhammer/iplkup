(ns test.logparse-test
  (:require [cljs.test :refer [deftest testing is run-tests]]
            [agip.logparse :as lp]))

(deftest logparse-test
  (testing "parse log and dateparser")
  (let [first-item (get (lp/parse-log "testdata/small.log") 0)]
    (is (= (:ip first-item) "54.245.183.198"))
    (is (= (:date first-item) #time/offset-date-time "2021-02-26T01:24:07Z"))))

(comment
  (cljs.test/run-tests)
  )

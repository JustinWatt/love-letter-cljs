(ns love-letter-cljs.core-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [love-letter-cljs.core :as core]))

(deftest fake-test
  (testing "fake description"
    (is (= 1 2))))

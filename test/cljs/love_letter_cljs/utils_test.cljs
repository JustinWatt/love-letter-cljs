(ns love-letter-cljs.utils-test
  (:require [cljs.test :refer-macros [run-tests deftest testing is]]
            [love-letter-cljs.utils :as sut]))

(def test-game-a
  {:deck '({:face :guard,    :value 1 :visible []}
           {:face :baron,    :value 3 :visible []}
           {:face :priest,   :value 2 :visible []}
           {:face :guard,    :value 1 :visible []}
           {:face :handmaid, :value 4 :visible []}
           {:face :prince,   :value 5 :visible []}
           {:face :handmaid, :value 4 :visible []}
           {:face :guard,    :value 1 :visible []}
           {:face :princess, :value 8 :visible []}
           {:face :baron,    :value 3 :visible []}
           {:face :king,     :value 6 :visible []}),
   :discard-pile [],
   :players {1 {:id 1, :hand [{:face :guard,  :value 1 :visible []}], :alive? true, :protected? true},
             2 {:id 2, :hand [{:face :priest, :value 2 :visible []}], :alive? false},
             3 {:id 3, :hand [{:face :guard,  :value 1 :visible []}], :alive? true},
             4 {:id 4, :hand [{:face :prince, :value 5 :visible []} {:face :countess, :value 7}], :alive? true}},
   :current-player 1})

(deftest find-card-test
  (is (= {:face :guard :value 1 :visible []}
         (-> test-game-a
             (sut/find-card 1)))))

(deftest valid-targets-test
  (is (= '(3 4)
         (-> test-game-a
             sut/valid-targets))))

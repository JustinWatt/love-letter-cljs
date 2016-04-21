(ns love-letter-cljs.handlers-test
  (:require [cljs.test :refer-macros [run-tests deftest testing is]]
            [love-letter-cljs.handlers :as sut]))

(def test-game-a
  {:deck [{:face :guard,    :value 1 :visible []}
          {:face :baron,    :value 3 :visible []}
          {:face :priest,   :value 2 :visible []}
          {:face :guard,    :value 1 :visible []}
          {:face :handmaid, :value 4 :visible []}
          {:face :prince,   :value 5 :visible []}
          {:face :handmaid, :value 4 :visible []}
          {:face :guard,    :value 1 :visible []}
          {:face :princess, :value 8 :visible []}
          {:face :baron,    :value 3 :visible []}
          {:face :king,     :value 6 :visible []}],
   :discard-pile [],
   :players {1 {:id 1, :hand [{:face :guard,  :value 1 :visible []}], :alive? true, :protected? true},
             2 {:id 2, :hand [{:face :priest, :value 2 :visible []}], :alive? false},
             3 {:id 3, :hand [{:face :guard,  :value 1 :visible []}], :alive? true},
             4 {:id 4, :hand [{:face :prince, :value 5 :visible []} {:face :countess, :value 7}], :alive? true}},
   :current-player 1})


(deftest start-next-turn-test
  (testing "if player 1 is current player and player 2 is dead, player 3 becomes next player"
    (is (= 3
           (-> test-game-a
               sut/start-next-turn
               :current-player)))))

(ns love-letter-cljs.handlers-test
  (:require [cljs.test :refer-macros [run-tests deftest testing is]]
            [love-letter-cljs.handlers :as sut]))

;; Fixtures
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
   :players {1 {:id 1,
                :hand [{:face :guard,  :value 1 :visible []}],
                :alive? true,
                :protected? true},
             2 {:id 2,
                :hand [{:face :priest, :value 2 :visible []}],
                :alive? false},
             3 {:id 3,
                :hand [{:face :guard,  :value 1 :visible []}],
                :alive? true},
             4 {:id 4,
                :hand [{:face :prince, :value 5 :visible []}
                       {:face :countess, :value 7, :visible []}],
                :alive? true}},
   :current-player 1})
(def test-game-b
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
   :players {1 {:id 1,
                :hand [{:face :guard,  :value 1 :visible []}],
                :alive? true,
                :protected? true},
             2 {:id 2,
                :hand [{:face :priest, :value 2 :visible []}],
                :alive? false},
             3 {:id 3,
                :hand [{:face :guard,  :value 1 :visible []}],
                :alive? true},
             4 {:id 4,
                :hand [{:face :prince, :value 5 :visible []}
                       {:face :countess, :value 7, :visible []}],
                :alive? true}},
   :current-player 3})

(def clean-slate
  {:deck
   [{:face :priest, :value 2, :visible []}
    {:face :handmaid, :value 4, :visible []}
    {:face :guard, :value 1, :visible []}
    {:face :priest, :value 2, :visible []}
    {:face :handmaid, :value 4, :visible []}
    {:face :prince, :value 5, :visible []}
    {:face :prince, :value 5, :visible []}
    {:face :guard, :value 1, :visible []}
    {:face :baron, :value 3, :visible []}
    {:face :countess, :value 7, :visible []}
    {:face :guard, :value 1, :visible []}],
   :debug-mode? true,
   :display-card nil,
   :phase :draw,
   :discard-pile [],
   :burn-pile [{:face :guard, :value 1, :visible []}],
   :card-target nil,
   :guard-guess nil,
   :active-card nil,
   :players {1
    {:id 1,
     :hand [{:face :princess, :value 8, :visible []}],
     :alive? true,
     :protected? false},
    2
    {:id 2,
     :hand [{:face :king, :value 6, :visible []}],
     :alive? true,
     :protected? false},
    3
    {:id 3,
     :hand [{:face :baron, :value 3, :visible []}],
     :alive? true,
     :protected? false},
    4
    {:id 4,
     :hand [{:face :guard, :value 1, :visible []}],
     :alive? true,
     :protected? false}},
   :log [{:from "System", :time "8:27:35 AM", :message "Welcome to the Game"}],
   :current-player 1} )

(deftest start-next-turn-test
  (testing "if player 1 is current player and player 2 is dead, player 3 becomes next player"
    (is (= 3 (-> test-game-a sut/start-next-turn :current-player)))
    (is (= 4 (-> test-game-b sut/start-next-turn :current-player)))))

(deftest set-guard-guess-test
  (testing "when setting a guard guess, it's placed correctly and transitions to resolution phase"
    (let [transitioned-game (sut/set-guard-guess-handler clean-slate [:king])]
      (is (= :resolution (:phase transitioned-game)))
      (is (= :king (:guard-guess transitioned-game))))))

(deftest set-active-card-test
  (testing "active card transition and update"
    (let [transitioned-game (sut/set-active-card-handler clean-slate [:king])]
      (is (= :king   (:active-card transitioned-game)))
      (is (= :target (:phase transitioned-game)))))
  (testing "special cases for cards that need no further state (target, guess, etc)"
    (is (= :resolution (-> clean-slate (sut/set-active-card-handler [:princess]) :phase)))))

(deftest set-target-test
  (testing "target transition and update"
    (let [transitioned-game (sut/set-target-handler clean-slate [3])]
      (is (= 3 (:card-target transitioned-game)))
      (is (= :resolution (:phase transitioned-game))))))

(deftest player-list-test
  (testing "extracts vector of living players"
    (is (= [1 3 4] (sut/player-list test-game-a)))))

(deftest next-in-list-test
  (testing "retrieves the next id in the list"
    (is (= 3 (sut/next-in-list [1 2 3 4] 2))))
  (testing "retrieves the first item if current id is the last element"
    (is (= 1 (sut/next-in-list [1 2 3 4] 4))))
  (testing "gets next greatest element if current element is missing"
    (is (= 4 (sut/next-in-list [1 2 4] 3))))
  (testing "gets first element if current element is greatest and missing"
    (is (= 1 (sut/next-in-list [1 2 3] 4)))))

(ns love-letter-cljs.game-test
  (:require [cljs.test :refer-macros [run-tests deftest testing is]]
            [love-letter-cljs.game :as sut]))

;; helpers
(defn discard-n [game n]
  (->> game
       (iterate #(sut/discard-card % [:deck]))
       (drop n)
       first))

;; (create-and-deal)
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

(def test-game-b
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
           {:face :prince,   :value 5 :visible []}
           {:face :king,     :value 6 :visible []}),
   :discard-pile [],
   :players {1 {:id 1, :hand [{:face :guard,  :value 1 :visible []}], :alive? true},
             2 {:id 2, :hand [{:face :priest, :value 2 :visible []}], :alive? true},
             3 {:id 3, :hand [{:face :guard,  :value 1 :visible []}], :alive? true},
             4 {:id 4, :hand [{:face :countess, :value 7 :visible []}], :alive? true}},
   :current-player 1})
;; tests

(deftest correct-card-count
  (is (= {:guard 5 :priest 2 :baron 2 :handmaid 2
          :prince 2 :king 1 :countess 1 :princess 1}
         (->> (sut/create-game)
              :deck
              (map :face)
              frequencies))))

(deftest baron-ability-test
  (testing "When a guard is compared to a priest the player with the guard is knocked out"
    (is (= false
           (-> test-game-a
               (sut/baron-ability 1 2)
               (get-in [:players 1 :alive?]))))))

(deftest guard-ability-test
  (testing "When the target's card is correctly guessed they are knocked out"
    (is (= true
           (-> test-game-a
               (sut/guard-ability :priest 2)
               (get-in [:players 2 :alive?]))))))

(deftest king-ability-test
  (testing "Target gains players card"
    (is (= [{:face :guard :value 1 :visible [1]}]
           (-> test-game-a
               (sut/king-ability 1 2)
               (get-in [:players 2 :hand])))))
  (testing "Player gains targets card"
    (is (= [{:face :priest :value 2 :visible [2]}]
           (-> test-game-a
               (sut/king-ability 1 2)
               (get-in [:players 1 :hand]))))))

(deftest discard-card-test
  (is (= [{:face :guard :value 1 :visible []}]
         (-> test-game-a
             (sut/discard-card [:players 1 :hand])
             :discard-pile))))

(deftest prince-ability-test
  (is (= [{:face :guard :value 1 :visible []}]
         (-> test-game-a
             (sut/prince-ability 2)
             (get-in [:players 2 :hand])))))

(deftest game-complete-non-empty-deck
  (is (= false
         (-> (sut/create-and-deal)
             (discard-n 10)
             sut/game-complete?))))

(deftest game-complete-empty-deck
  (is (= true
         (-> (sut/create-and-deal)
             (discard-n 12)
             sut/game-complete?))))

(deftest countess-check-test
  (is (= true
         (-> test-game-a
             (sut/countess-check 4)))))

(deftest remove-protection-test
  (is (= false
         (-> test-game-a
             sut/remove-protection
             (get-in [:players 1 :protected?])))))

(deftest count-alive-test
  (is (= 3 (-> test-game-a sut/count-alive))))

(deftest reveal-card-to-player-test
  (is (= [1]
         (-> test-game-a
             (sut/reveal-card-to-player 1 2)
             (get-in [:players 2 :hand])
             (->> first
                  :visible)))))

(deftest move-card-test
  (is (= {:face :guard,:value 1 :visible []} 
         (-> test-game-a
             (sut/move-card [:deck] [:discard-pile])
             (get-in [:discard-pile])
             first))))


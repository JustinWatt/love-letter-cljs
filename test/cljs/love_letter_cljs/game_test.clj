(ns love-letter-cljs.game-test
  (:require [clojure.test :refer :all]
            [love-letter-cljs.game :refer :all]))

;; helpers
(defn discard-n [game n]
  (->> game
       (iterate #(discard-card % [:deck]))
       (drop n)
       first))

;; (create-and-deal)
(def test-game-a
  {:deck '({:face :guard,    :value 1}
           {:face :baron,    :value 3}
           {:face :priest,   :value 2}
           {:face :guard,    :value 1}
           {:face :handmaid, :value 4}
           {:face :prince,   :value 5}
           {:face :handmaid, :value 4}
           {:face :guard,    :value 1}
           {:face :princess, :value 8}
           {:face :baron,    :value 3}
           {:face :king,     :value 6}),
   :discard-pile [],
   :players {1 {:id 1, :hand [{:face :guard,  :value 1}], :alive? true},
             2 {:id 2, :hand [{:face :priest, :value 2}], :alive? false},
             3 {:id 3, :hand [{:face :guard,  :value 1}], :alive? true},
             4 {:id 4, :hand [{:face :prince, :value 5} {:face :countess, :value 7}], :alive? true}},
   :current-player 1})

(def test-game-b
  {:deck '({:face :guard,    :value 1}
           {:face :baron,    :value 3}
           {:face :priest,   :value 2}
           {:face :guard,    :value 1}
           {:face :handmaid, :value 4}
           {:face :prince,   :value 5}
           {:face :handmaid, :value 4}
           {:face :guard,    :value 1}
           {:face :princess, :value 8}
           {:face :baron,    :value 3}
           {:face :prince,   :value 5}
           {:face :king,     :value 6}),
   :discard-pile [],
   :players {1 {:id 1, :hand [{:face :guard,  :value 1}], :alive? true},
             2 {:id 2, :hand [{:face :priest, :value 2}], :alive? true},
             3 {:id 3, :hand [{:face :guard,  :value 1}], :alive? true},
             4 {:id 4, :hand [{:face :countess, :value 7}], :alive? true}},
   :current-player 1})
;; tests

(deftest correct-card-count
  (is (= {:guard 5 :priest 2 :baron 2 :handmaid 2
          :prince 2 :king 1 :countess 1 :princess 1}
         (->> (create-game)
              :deck
              (map :face)
              frequencies))))

(deftest baron-ability-test
  (testing "When a guard is compared to a priest the player with the guard is knocked out"
      (is (= false
             (-> test-game-a
                 (baron-ability 1 2)
                 (get-in [:players 1 :alive?]))))))

(deftest guard-ability-test
  (testing "When the target's card is correctly guessed they are knocked out"
    (is (= true
           (-> test-game-a
               (guard-ability :priest 2)
               (get-in [:players 2 :alive?]))))))

(deftest king-ability-test
  (is (= {:face :guard :value 1}
         (-> test-game-a
             (king-ability 1 2)
             (get-in [:players 2 :hand])))))

(deftest discard-card-test
  (is (= [{:face :guard :value 1}]
         (-> test-game-a
             (discard-card [:players 1 :hand])
             :discard-pile))))

(deftest prince-ability-test
  (is (= [{:face :guard :value 1}]
         (-> test-game-a
             (discard-card [:players 2 :hand])
             (draw-card 2)
             (get-in [:players 2 :hand])))))

(deftest game-complete-non-empty-deck
  (is (= false
         (-> (create-and-deal)
             (discard-n 11)
             game-complete?))))

(deftest game-complete-empty-deck
  (is (= true
         (-> (create-and-deal)
             (discard-n 12)
             game-complete?))))

(deftest countess-check-test
  (is (= true
         (-> test-game-a
             (countess-check 4)))))

(deftest countess-check-test
  (is (= true
         (-> test-game-a
             (countess-check 4)))))

(deftest valid-targets-test
  (is (= '(3 4)
         (-> test-game-a
             valid-targets))))


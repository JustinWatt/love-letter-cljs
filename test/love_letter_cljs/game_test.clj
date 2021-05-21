(ns love-letter-cljs.game-test
  (:require [clojure.test :refer :all]
            [love-letter-cljs.game :as sut]))

;; helpers
(defn discard-n [game n]
  (->> game
       (iterate #(sut/move-card % [:game/deck] [:game/discard-pile]))
       (drop n)
       first))

;; (create-and-deal)
(def test-game-a
  {:game/deck [{:card/face :guard,    :card/value 1 :card/visible []}
               {:card/face :baron,    :card/value 3 :card/visible []}
               {:card/face :priest,   :card/value 2 :card/visible []}
               {:card/face :guard,    :card/value 1 :card/visible []}
               {:card/face :handmaid, :card/value 4 :card/visible []}
               {:card/face :prince,   :card/value 5 :card/visible []}
               {:card/face :handmaid, :card/value 4 :card/visible []}
               {:card/face :guard,    :card/value 1 :card/visible []}
               {:card/face :princess, :card/value 8 :card/visible []}
               {:card/face :baron,    :card/value 3 :card/visible []}
               {:card/face :king,     :card/value 6 :card/visible []}],
   :game/discard-pile [],
   :game/players {1 {:player/id 1, :player/hand [{:card/face :guard,  :card/value 1 :card/visible []}], :player/alive? true, :player/protected? true},
                  2 {:player/id 2, :player/hand [{:card/face :priest, :card/value 2 :card/visible []}], :player/alive? false},
                  3 {:player/id 3, :player/hand [{:card/face :guard,  :card/value 1 :card/visible []}], :player/alive? true},
                  4 {:player/id 4, :player/hand [{:card/face :prince, :card/value 5 :card/visible []} {:card/face :countess, :card/value 7}], :player/alive? true}},
   :game/current-player 1})

(def test-game-b
  {:game/deck [{:card/face :guard,    :card/value 1 :card/visible []}
               {:card/face :baron,    :card/value 3 :card/visible []}
               {:card/face :priest,   :card/value 2 :card/visible []}
               {:card/face :guard,    :card/value 1 :card/visible []}
               {:card/face :handmaid, :card/value 4 :card/visible []}
               {:card/face :prince,   :card/value 5 :card/visible []}
               {:card/face :handmaid, :card/value 4 :card/visible []}
               {:card/face :guard,    :card/value 1 :card/visible []}
               {:card/face :princess, :card/value 8 :card/visible []}
               {:card/face :baron,    :card/value 3 :card/visible []}
               {:card/face :prince,   :card/value 5 :card/visible []}
               {:card/face :king,     :card/value 6 :card/visible []}],
   :game/discard-pile [],
   :game/players {1 {:player/id 1, :player/hand [{:card/face :guard,  :card/value 1 :card/visible []}], :player/alive? true},
                  2 {:player/id 2, :player/hand [{:card/face :priest, :card/value 2 :card/visible []}], :player/alive? true},
                  3 {:player/id 3, :player/hand [{:card/face :guard,  :card/value 1 :card/visible []}], :player/alive? true},
                  4 {:player/id 4, :player/hand [{:card/face :countess, :card/value 7 :card/visible []}], :player/alive? true}},
   :game/current-player 1})
;; tests

(deftest correct-card-count
  (is (= {:guard 5 :priest 2 :baron 2 :handmaid 2
          :prince 2 :king 1 :countess 1 :princess 1}
         (->> (sut/create-game)
              :game/deck
              (map :card/face)
              frequencies))))

(deftest baron-ability-test
  (testing "When a guard is compared to a priest the player with the guard is knocked out"
    (is (= false
           (-> test-game-a
               (sut/baron-ability 1 2)
               (get-in [:game/players 1 :player/alive?]))))))

(deftest guard-ability-test
  (testing "When the target's card is correctly guessed they are knocked out"
    (is (= false
           (-> test-game-a
               (sut/guard-ability :priest 2)
               (get-in [:game/players 2 :player/alive?]))))))

(deftest king-ability-test
  (testing "Target gains players card"
    (is (= [{:card/face :guard :card/value 1 :card/visible [1]}]
           (-> test-game-a
               (sut/king-ability 1 2)
               (get-in [:game/players 2 :player/hand])))))
  (testing "Player gains targets card"
    (is (= [{:card/face :priest :card/value 2 :card/visible [2]}]
           (-> test-game-a
               (sut/king-ability 1 2)
               (get-in [:game/players 1 :player/hand]))))))

(deftest prince-ability-test
  (is (= [{:card/face :guard :card/value 1 :card/visible []}]
         (-> test-game-a
             (sut/prince-ability 2)
             (get-in [:game/players 2 :player/hand])))))

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
             (sut/countess-check 4))))
  (is (= false
         (-> test-game-a
             (sut/countess-check 3)))))

(deftest remove-protection-test
  (is (= false
         (-> test-game-a
             sut/remove-protection
             (get-in [:game/players 1 :player/protected?])))))

(deftest count-alive-test
  (is (= 3 (-> test-game-a sut/count-alive))))

(deftest reveal-card-to-player-test
  (is (= [1]
         (-> test-game-a
             (sut/reveal-card-to-player 1 2)
             (get-in [:game/players 2 :player/hand])
             first
             :card/visible))))

(deftest move-card-test
  (is (= {:card/face :guard :card/value 1 :card/visible []}
         (-> test-game-a
             (sut/move-card [:game/deck] [:game/discard-pile])
             (get-in [:game/discard-pile])
             first))))

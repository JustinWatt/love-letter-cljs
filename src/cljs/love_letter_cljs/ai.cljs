(ns love-letter-cljs.ai
  (:require [love-letter-cljs.utils :as u]))

(def action-types [:suicide
                   :eliminate
                   :assist
                   :survive
                   :high-card
                   :defensive :bluff])

;;; AI STUFF
(def complete-deck
  [{:face :handmaid :value 4 :visible []}
   {:face :guard    :value 1 :visible []}
   {:face :baron    :value 3 :visible []}
   {:face :prince   :value 5 :visible []}
   {:face :priest   :value 2 :visible []}
   {:face :guard    :value 1 :visible []}
   {:face :handmaid :value 4 :visible []}
   {:face :king     :value 6 :visible []}
   {:face :prince   :value 5 :visible []}
   {:face :countess :value 7 :visible []}
   {:face :guard    :value 1 :visible []}
   {:face :guard    :value 1 :visible []}
   {:face :baron    :value 3 :visible []}
   {:face :princess :value 8 :visible []}
   {:face :priest   :value 2 :visible []}
   {:face :guard    :value 1 :visible []}])

(def test-game
  {:deck [{:face :priest   :value 2}
          {:face :baron    :value 3}
          {:face :prince   :value 5}
          {:face :countess :value 7}
          {:face :guard    :value 1}]

   :discard-pile [{:face :guard :value 1}
                  {:face :guard :value 1}
                  {:face :prince :value 5}
                  {:face :priest :value 2}
                  {:face :handmaid :value 4}]

   :burn-pile [{:face :handmaid :value 4}]

   :players {1 {:id 1
                :hand [{:face :king  :value 6 :visible []}
                       {:face :baron :value 3 :visible []}]
                :alive? true
                :protected? false}

             2 {:id 2
                :hand [{:face :guard :value 1 :visible [1]}]
                :alive? true
                :protected? false}

             3 {:id 3
                :hand [{:face :princess :value 8 :visible []}]
                :alive? true
                :protected? false}

             4 {:id 4
                :hand [{:face :guard :value 1 :visible []}]
                :alive? true :protected? true}}

   :current-player 1})

(defn known-card [game player target]
  (let [target-card (get-in game [:players target :hand 0])
        visible-set (set (:visible target-card))]
    (if (contains? visible-set player)
      target-card
      nil)))

(defn player-hand [game id]
  (get-in game [:players id :hand]))


(defn filter-fresh-deck [card-list]
  (reduce (fn [deck card]
           (u/remove-first card deck)) complete-deck card-list))

(defn high-card [game card]
  (let [{:keys [discard-pile current-player]} game
        {:keys [face value]} card
        current-player-hand (player-hand game current-player)
        known-list          (vec (map :face (concat discard-pile current-player-hand)))
        filtered-deck       (filter-fresh-deck known-list)]
    (* 100 (/ (count (filter #(>= value (:value %)) filtered-deck))
              (count filtered-deck)))))

;; Game -> Id -> Card -> Float
(defn guard-probability [game target-player guess]
  (let [{:keys [discard-pile current-player]} game
        guess-face (:face guess)
        current-player-hand (player-hand game current-player)
        known-list          (mapv :face (concat discard-pile current-player-hand))
        visible-card        (known-card game current-player target-player)
        filtered-deck       (filter-fresh-deck known-list)]
    (if visible-card
      (if (= (:face visible-card) guess-face) 100 0)
      (* 100 (/ (count (filter #(= guess-face (:face %)) filtered-deck))
                (count filtered-deck))))))

(defn baron-probability [game player-card target-player]
  (let [{:keys [discard-pile current-player]} game
        player-value        (:value player-card)
        current-player-hand (player-hand game current-player)
        known-list          (mapv :face (concat discard-pile current-player-hand))
        visible-card        (known-card game current-player target-player)
        filtered-deck       (filter-fresh-deck known-list)]
    (if visible-card
      (if (> player-value (:value visible-card)) 100 0)
      (* 100 (/ (count (filter #(> player-value (:value %)) filtered-deck))
                (count filtered-deck))))))

(defn baron-survival-probability [game player-card target-player]
  (let [{:keys [discard-pile current-player]} game
        player-value        (:value player-card)
        current-player-hand (player-hand game current-player)
        known-list          (mapv :face (concat discard-pile current-player-hand))
        visible-card        (known-card game current-player target-player)
        filtered-deck       (filter-fresh-deck known-list)]
    (if visible-card
      (if (>= player-value (:value visible-card)) 100 0)
      (* 100 (/ (count (filter #(>= player-value (:value %)) filtered-deck))
                (count filtered-deck))))))

(defn prince-probability [game target-player]
  (let [{:keys [discard-pile current-player]} game
        current-player-hand (player-hand game current-player)
        visible-card        (known-card game current-player target-player)
        known-list          (mapv :face (concat discard-pile current-player-hand))
        filtered-deck       (filter-fresh-deck known-list)]
    (if visible-card
      (if (= :princess (:face visible-card)) 100 0)
      (* 100 (/ (count (filter #(= :princess (:face %)) filtered-deck))
                (count filtered-deck))))))

(defn princess-suicide-probability [] -1)

(defn get-other-card [card hand]
  (peek (u/remove-first card hand)))

(defn king-assist-probability [game target-player]
  (let [{:keys [current-player]} game
        current-player-hand (player-hand game current-player)
        visible-card        (known-card game current-player target-player)
        players-other-card  (get-other-card :king current-player-hand)]
    (if visible-card
      (case (:face visible-card)
        :guard     80
        :priest     0
        :baron     40
        :handmaid 100
        :prince    30
        :countess  10
        :princess   0
        :default    0)
      (case (:face players-other-card)
        :guard      0
        :priest    90
        :baron     10
        :handmaid   0
        :prince    25
        :princess 100
        :default    0))))

(defn contains-attack-card? [hand]
  (let [faces (set (map :face hand))]
    (some faces [:baron :guard :prince])))

(defn priest-probability [game target-player]
  (let [{:keys [current-player]} game
        current-player-hand (player-hand game current-player)
        visible-card        (known-card game current-player target-player)]
    (if visible-card
      0
      (if (contains-attack-card? current-player-hand) 100 50))))

(defn holding-princess? [hand]
  (= :princess (:face (get-other-card :countess hand))))

(defn countess-bluff [game]
  (let [{:keys [current-player]} game
        current-player-hand (player-hand game current-player)]
    (if (holding-princess? current-player-hand) 0 100)))

(defn handmaid-defensive-probability [] 100)

(def card-faces
  #{:guard
    :priest
    :baron
    :handmaid
    :prince
    :king
    :countess
    :princess})

(defn guard-prob-inputs [game]
  (let [current-player (:current-player game)
        valid-targets (u/valid-targets game)
        faces   (remove #{:guard} card-faces)]
    (for [t valid-targets
          f faces]
      [t f])))

(defn guard-elimination-action
  [game [target guess]]
  {:action {:current-player (:current-player game)
            :active-card :guard
            :target target
            :guard-guess guess}
   :type :eliminate
   :strength (guard-probability game target guess)})

(defn generate-high-card-action [game card]
  {:action {:current-player (:current-player game)
            :active-card (:face card)
            :target nil
            :guard-guess nil}
   :type :high-card
   :strength (high-card game card)})

(defn generate-actions [game card]
  (conj (mapv #(guard-elimination-action game %) (guard-prob-inputs game))
        (generate-high-card-action game card)))

#_[{:action {:target nil
           :guard-guess nil
           :active-card nil
           :current-player nil}
  :type :defense
  :strength 100}]


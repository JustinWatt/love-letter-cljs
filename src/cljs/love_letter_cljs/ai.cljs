(ns love-letter-cljs.ai)


(def cards [{:face :guard    :value 1 :count 5}
            {:face :priest   :value 2 :count 2}
            {:face :baron    :value 3 :count 2}
            {:face :handmaid :value 4 :count 2}
            {:face :prince   :value 5 :count 2}
            {:face :king     :value 6 :count 1}
            {:face :countess :value 7 :count 1}
            {:face :princess :value 8 :count 1}])

(def action-types [:suicide
                   :eliminate
                   :assist
                   :survive
                   :high-card
                   :defensive :bluff])

(defn generate-card [card]
  (let [{:keys [count face value]} card]
    (repeat count {:face face :value value})))

(defn generate-deck []
  (vec (shuffle (mapcat generate-card cards))))

;;; AI STUFF

(defn remove-first [face coll]
  (let [[pre post] (split-with #(not= face (:face %)) coll)]
    (vec (concat pre (rest post)))))


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
           (remove-first card deck)) (generate-deck) card-list))

(defn high-card [game card]
  (let [{:keys [discard-pile current-player]} game
        {:keys [face value]} card
        current-player-hand (player-hand game current-player)
        known-list (vec (map :face (concat discard-pile current-player-hand)))
        filtered-deck (filter-fresh-deck known-list)]
    (float (/ (count (filter #(>= value (:value %)) filtered-deck))
              (count filtered-deck)))))

(map apply [inc dec (comp inc inc)] [1 2 3])

;; Game -> Id -> Card -> Float
(defn guard-probability [game target-player guess]
  (let [{:keys [discard-pile current-player]} game
        guess-face (:face guess)
        current-player-hand (player-hand game current-player)
        known-list (mapv :face (concat discard-pile current-player-hand))
        visible-card (known-card game current-player target-player)
        filtered-deck (filter-fresh-deck known-list)]
    (if visible-card
      (if (= (:face visible-card) guess-face) 100 0)
      (float (/ (count (filter #(= guess-face (:face %)) filtered-deck))
                (count filtered-deck))))))

(guard-probability test-game 2 {:face :guard :value 3})
(guard-probability test-game 2 {:face :baron :value 3})
(guard-probability test-game 3 {:face :baron :value 3})

(defn baron-probability [game player-card target-player]
  (let [{:keys [discard-pile current-player]} game
        player-value        (:value player-card)
        current-player-hand (player-hand game current-player)
        known-list          (mapv :face (concat discard-pile current-player-hand))
        visible-card        (known-card game current-player target-player)
        filtered-deck       (filter-fresh-deck known-list)]
    (if visible-card
      (if (> player-value (:value visible-card)) 100 0)
      (float (/ (count (filter #(> player-value (:value %)) filtered-deck))
                (count filtered-deck))))))

(baron-probability test-game {:face :king :value 6} 2)

(defn baron-survival-probability [game player-card target-player]
  (let [{:keys [discard-pile current-player]} game
        player-value        (:value player-card)
        current-player-hand (player-hand game current-player)
        known-list          (mapv :face (concat discard-pile current-player-hand))
        visible-card        (known-card game current-player target-player)
        filtered-deck       (filter-fresh-deck known-list)]
    (if visible-card
      (if (>= player-value (:value visible-card)) 100 0)
      (float (/ (count (filter #(>= player-value (:value %)) filtered-deck))
                (count filtered-deck))))))

(baron-survival-probability test-game {:face :king :value 6} 2)

(defn prince-probability [game target-player]
  (let [{:keys [discard-pile current-player]} game
        current-player-hand (player-hand game current-player)
        visible-card        (known-card game current-player target-player)
        known-list          (mapv :face (concat discard-pile current-player-hand))
        filtered-deck       (filter-fresh-deck known-list)]
    (if visible-card
      (if (= :princess (:face visible-card)) 100 0)
      (float (/ (count (filter #(= :princess (:face %)) filtered-deck))
                (count filtered-deck))))))

(defn princess-suicide-probability [] -1)

(defn get-other-card [card hand]
  (first (remove-first card hand)))

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

(priest-probability test-game 4)
(prince-probability test-game 3)

(high-card test-game {:face :king :value 6})

(defn holding-princess? [hand]
  (= :princess (:face (get-other-card :countess hand))))

(defn countess-bluff [game]
  (let [{:keys [current-player]} game
        current-player-hand (player-hand game current-player)]
    (if (holding-princess? current-player-hand) 0 100)))

(defn handmaid-defensive-probability [] 100)


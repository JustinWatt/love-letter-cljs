(ns love-letter-cljs.ai
  (:require [love-letter-cljs.utils :as u]
            [re-frame.core :as re-frame]))

(def action-types [:suicide
                   :eliminate
                   :assist
                   :survive
                   :high-card
                   :defensive
                   :bluff])

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
                :protected? false
                :personality :base}

             2 {:id 2
                :hand [{:face :guard :value 1 :visible []}]
                :alive? true
                :protected? false
                :personality :aggressive}

             3 {:id 3
                :hand [{:face :princess :value 8 :visible [1]}]
                :alive? true
                :protected? false
                :personality :aggressive}

             4 {:id 4
                :hand [{:face :guard :value 1 :visible []}]
                :alive? true
                :protected? true
                :personality :defensive}}

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
(defn guard-probability [game target-player guess-face]
  (let [{:keys [discard-pile current-player]} game
        current-player-hand (player-hand game current-player)
        known-list          (mapv :face (concat discard-pile current-player-hand))
        visible-card        (known-card game current-player target-player)
        filtered-deck       (filter-fresh-deck known-list)]
    (if visible-card
      (if (= (:face visible-card) guess) 100 0)
      (* 100 (/ (count (filter #(= guess (:face %)) filtered-deck))
                (count filtered-deck))))))

(guard-probability test-game 3 :countess)

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
        :countess   0
        :default    0))))

(defn- contains-attack-card? [hand]
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

(defn- guard-prob-inputs [game]
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
            :card-target target
            :guard-guess guess}
   :type :eliminate
   :strength (guard-probability game target guess)})

(guard-elimination-action test-game [3 :king])

(defn generate-high-card-action [game card]
  {:action {:current-player (:current-player game)
            :active-card (:face card)
            :card-target nil
            :guard-guess nil}
   :type :high-card
   :strength (high-card game card)})

(defn priest-assist-action [game target-id]
  {:action {:current-player (:current-player game)
            :active-card :priest
            :card-target target-id
            :guard-guess nil}
   :type :assist
   :strength (priest-probability game target-id)})

(defn baron-eliminate-action [game player-card target-id]
  {:action {:current-player (:current-player game)
            :active-card :baron
            :card-target target-id
            :guard-guess nil}
   :type :eliminate
   :strength (baron-probability game player-card target-id)})

(defn baron-survival-action [game player-card target-id]
  {:action {:current-player (:current-player game)
            :active-card :baron
            :card-target target-id
            :guard-guess nil}
   :type :survive
   :strength (baron-survival-probability game player-card target-id)})

(defn handmaid-defensive-action [game]
  {:action {:current-player (:current-player game)
            :active-card :handmaid
            :card-target nil
            :guard-guess nil}
   :type :defensive
   :strength (handmaid-defensive-probability)})

(defn prince-eliminate-action [game target-id]
  {:action {:current-player (:current-player game)
            :active-card :prince
            :card-target target-id
            :guard-guess nil}
   :type :eliminate
   :strength (prince-probability game target-id)})

(defn king-assist-action [game target-id]
  {:action {:current-player (:current-player game)
            :active-card :king
            :card-target target-id
            :guard-guess nil}
   :type :assist
   :strength (king-assist-probability game target-id)} )

(defn countess-bluff-action [game]
  {:action {:current-player (:current-player game)
            :active-card :countess
            :card-target nil
            :guard-guess nil}
   :type :bluff
   :strength (countess-bluff game)})

(defn princess-suicide-action [game]
  {:action {:current-player (:current-player game)
            :active-card :princess
            :card-target nil
            :guard-guess nil}
   :type :suicide
   :strength (princess-suicide-probability)})

(defn generate-card-actions [game card]
  (let [high-card-action (generate-high-card-action game card)
        targets (u/valid-targets game)]
    (cons high-card-action
          (case (:face card)
            :guard  (map #(guard-elimination-action game %) (guard-prob-inputs game))
            :priest (map #(priest-assist-action game %) targets)
            :baron  (concat (map #(baron-survival-action game card %) targets)
                            (map #(baron-eliminate-action game card %) targets))
            :handmaid [(handmaid-defensive-action game)]
            :prince   (map #(prince-eliminate-action game %) targets)
            :king     (map #(king-assist-action game %) targets)
            :countess [(countess-bluff-action game)]
            :princess [(princess-suicide-action game)]
            :default  []))))

(defn normalize-hand [hand]
  (vec (set (map #(select-keys % [:face :value]) hand))))

(def personality-profiles
  {:aggressive {:eliminate .80
                :assist    .70
                :high-card .40
                :defensive .40
                :survive   .25
                :bluff     .15
                :suicide  1}

   :defensive  {:eliminate .40
                :assist    .25
                :high-card .40
                :defensive .80
                :survive   .80
                :bluff     .15
                :suicide  1}

   :base       {:eliminate 1
                :assist    1
                :high-card 1
                :defensive 1
                :survive   1
                :bluff     1
                :suicide   1}})

(defn apply-personality [personality action]
  (let [type (:type action)]
    (update action :strength * (type (personality personality-profiles)))))

(defn generate-actions [game player-id]
  (let [personality (get-in game [:players player-id :personality])]
    (->> (mapcat #(generate-card-actions game %) (normalize-hand (player-hand game player-id)))
         (map #(apply-personality personality %))
         (sort-by :strength)
         reverse)))

(defn other-card-actions [best-action actions]
  (filter #(and (not= (:type %) :high-card)
                (not= (:active-card (:action %))
                      (:active-card (:action best-action)))) actions))

(defn other-card-face [best-action actions]
  (:active-card (:action (first (filter #(not= (:active-card (:action best-action)) (:active-card (:action %))) actions)))))

(defn same-card-actions [best-action actions]
  (filter #(and (not= (:type %) :high-card)
                (= (:active-card (:action %))
                   (:active-card (:action best-action)))) actions))

(defn no-op-action [face current-player]
  {:action {:active-card face
            :card-target nil
            :current-player current-player
            :guard-guess nil}})

(defn pick-action [actions]
  (let [best-action   (first actions)]
    (if (not= :high-card (:type best-action))
      best-action
      (let [other-actions (other-card-actions best-action actions)
            other-face    (other-card-face best-action actions)
            same-actions  (same-card-actions best-action actions)
            {:keys [active-card current-player]} (:action best-action)]
        (if (nil? other-face)
          (if (empty? same-actions)
            (no-op-action active-card current-player)
            (first same-actions))
          (if (empty? other-actions)
            (no-op-action other-face current-player)
            (first other-actions)))))))


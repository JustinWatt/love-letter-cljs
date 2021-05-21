(ns love-letter-cljs.ai
  (:require [love-letter-cljs.utils :as u]
            [clojure.set :as set]))

(def personalities
  [:aggressive :base :defensive])

(def complete-deck
  [{:card/face :handmaid :card/value 4 :card/visible []}
   {:card/face :guard    :card/value 1 :card/visible []}
   {:card/face :baron    :card/value 3 :card/visible []}
   {:card/face :prince   :card/value 5 :card/visible []}
   {:card/face :priest   :card/value 2 :card/visible []}
   {:card/face :guard    :card/value 1 :card/visible []}
   {:card/face :handmaid :card/value 4 :card/visible []}
   {:card/face :king     :card/value 6 :card/visible []}
   {:card/face :prince   :card/value 5 :card/visible []}
   {:card/face :countess :card/value 7 :card/visible []}
   {:card/face :guard    :card/value 1 :card/visible []}
   {:card/face :guard    :card/value 1 :card/visible []}
   {:card/face :baron    :card/value 3 :card/visible []}
   {:card/face :princess :card/value 8 :card/visible []}
   {:card/face :priest   :card/value 2 :card/visible []}
   {:card/face :guard    :card/value 1 :card/visible []}])

(defn known-card [game player target]
  (let [target-card (get-in game [:game/players target :player/hand 0])
        visible-set (set (:card/visible target-card))]
    (when (contains? visible-set player)
      target-card)))

(defn player-hand [game id]
  (get-in game [:game/players id :player/hand]))

(defn filter-fresh-deck [card-list]
  (reduce u/remove-first complete-deck card-list))

(defn high-card
  "Probability of a given card being the most valuable card left in the game."
  [game card]
  (let [{:game/keys [discard-pile current-player]} game
        {:card/keys [value]} card
        current-player-hand (player-hand game current-player)
        known-list          (vec (map :card/face (concat discard-pile current-player-hand)))
        filtered-deck       (filter-fresh-deck known-list)]
    (* 100 (/ (count (filter #(>= value (:card/value %)) filtered-deck))
              (count filtered-deck)))))

(defn guard-probability
  "Probability of a given guess being correct."
  [game target-player guess]
  (let [{:game/keys [discard-pile current-player]} game
        current-player-hand (player-hand game current-player)
        known-list          (mapv :card/face (concat discard-pile current-player-hand))
        visible-card        (known-card game current-player target-player)
        filtered-deck       (filter-fresh-deck known-list)]
    (if visible-card
      (if (= (:card/face visible-card) guess) 100 0)
      (* 100 (/ (count (filter #(= guess (:card/face %)) filtered-deck))
                (count filtered-deck))))))

(defn baron-probability
  "Probability of the player's card beating an opponent's card.
   If the card is visible the probability is certain."
  [game player-card target-player]
  (let [{:game/keys [discard-pile current-player]} game
        player-value        (:card/value player-card)
        current-player-hand (player-hand game current-player)
        known-list          (mapv :card/face (concat discard-pile current-player-hand))
        visible-card        (known-card game current-player target-player)
        filtered-deck       (filter-fresh-deck known-list)]
    (if visible-card
      (if (> player-value (:card/value visible-card)) 100 0)
      (* 100 (/ (count (filter #(> player-value (:card/value %)) filtered-deck))
                (count filtered-deck))))))

(defn baron-survival-probability
  "Probability of have a greater or equal valued card as a target player."
  [game player-card target-player]
  (let [{:game/keys [discard-pile current-player]} game
        player-value        (:card/value player-card)
        current-player-hand (player-hand game current-player)
        known-list          (mapv :card/face (concat discard-pile current-player-hand))
        visible-card        (known-card game current-player target-player)
        filtered-deck       (filter-fresh-deck known-list)]
    (if visible-card
      (if (>= player-value (:card/value visible-card)) 100 0)
      (* 100 (/ (count (filter #(>= player-value (:card/value %)) filtered-deck))
                (count filtered-deck))))))

(defn prince-probability
  "Probability that target player is holding the princess."
  [game target-player]
  (let [{:game/keys [discard-pile current-player]} game
        current-player-hand (player-hand game current-player)
        visible-card        (known-card game current-player target-player)
        known-list          (mapv :card/face (concat discard-pile current-player-hand))
        filtered-deck       (filter-fresh-deck known-list)]
    (if visible-card
      (if (= :princess (:card/face visible-card)) 100 0)
      (* 100 (/ (count (filter #(= :princess (:card/face %)) filtered-deck))
                (count filtered-deck))))))

(defn princess-suicide-probability
  "Probability for the player to discard the princess on purpose"
  [] -1)

(defn get-other-card [hand card]
  (peek (u/remove-first hand card)))

(defn king-assist-probability
  "If the opponents card is known, how valuable trading for it would be.
   If it's unknown, how beneficial getting rid of the players other card would be."
  [game target-player]
  (let [{:game/keys [current-player]} game
        current-player-hand (player-hand game current-player)
        visible-card        (known-card game current-player target-player)
        players-other-card  (get-other-card current-player-hand :king)]
    (if visible-card
      (case (:card/face visible-card)
        :guard     80
        :priest     0
        :baron     40
        :handmaid 100
        :prince    30
        :countess  10
        :princess   0
        :default    0)
      (case (:card/face players-other-card)
        :guard      0
        :priest    90
        :baron     10
        :handmaid   0
        :prince    25
        :princess 100
        :countess   0
        :default    0))))

(defn- contains-attack-card?
  [hand]
  (-> (set (map :card/face hand))
      (set/intersection #{:baron :guard :prince})
      seq))

(defn priest-probability
  "Returns how valuable the knowledge gained by the priest would be."
  [game target-player]
  (let [{:game/keys [current-player]} game
        current-player-hand (player-hand game current-player)
        visible-card        (known-card game current-player target-player)]
    (if visible-card
      0
      (if (contains-attack-card? current-player-hand)
        100
        50))))

(defn holding-princess? [hand]
  (= :princess (:card/face (get-other-card hand :countess))))

(defn countess-bluff [game]
  (let [{:game/keys [current-player]} game
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

(defn- guard-prob-inputs
  "Generates a list of pairs containing a target and a guess."
  [game]
  (let [valid-targets (u/valid-targets game)
        faces   (remove #{:guard} card-faces)]
    (for [t valid-targets
          f faces]
      [t f])))


; Functions for generating card actions


(defn guard-elimination-action
  [game [target guess]]
  {:action {:game/current-player (:game/current-player game)
            :active-card :guard
            :card-target target
            :guard-guess guess}
   :type :eliminate
   :strength (guard-probability game target guess)})

(defn generate-high-card-action [game card]
  {:action {:game/current-player (:game/current-player game)
            :active-card (:card/face card)
            :card-target nil
            :guard-guess nil}
   :type :high-card
   :strength (high-card game card)})

(defn priest-assist-action [game target-id]
  {:action {:game/current-player (:game/current-player game)
            :active-card :priest
            :card-target target-id
            :guard-guess nil}
   :type :assist
   :strength (priest-probability game target-id)})

(defn baron-eliminate-action [game player-card target-id]
  {:action {:game/current-player (:game/current-player game)
            :active-card :baron
            :card-target target-id
            :guard-guess nil}
   :type :eliminate
   :strength (baron-probability game player-card target-id)})

(defn baron-survival-action [game player-card target-id]
  {:action {:game/current-player (:game/current-player game)
            :active-card :baron
            :card-target target-id
            :guard-guess nil}
   :type :survive
   :strength (baron-survival-probability game player-card target-id)})

(defn handmaid-defensive-action [game]
  {:action {:game/current-player (:game/current-player game)
            :active-card :handmaid
            :card-target nil
            :guard-guess nil}
   :type :defensive
   :strength (handmaid-defensive-probability)})

(defn prince-eliminate-action [game target-id]
  {:action {:game/current-player (:game/current-player game)
            :active-card :prince
            :card-target target-id
            :guard-guess nil}
   :type :eliminate
   :strength (prince-probability game target-id)})

(defn king-assist-action [game target-id]
  {:action {:game/current-player (:game/current-player game)
            :active-card :king
            :card-target target-id
            :guard-guess nil}
   :type :assist
   :strength (king-assist-probability game target-id)})

(defn countess-bluff-action [game]
  {:action {:game/current-player (:game/current-player game)
            :active-card :countess
            :card-target nil
            :guard-guess nil}
   :type :bluff
   :strength (countess-bluff game)})

(defn princess-suicide-action [game]
  {:action {:game/current-player (:game/current-player game)
            :active-card :princess
            :card-target nil
            :guard-guess nil}
   :type :suicide
   :strength (princess-suicide-probability)})

(defn generate-card-actions
  "Generates actions for each card"
  [game card]
  (let [high-card-action (generate-high-card-action game card)
        targets (u/valid-targets game)]
    (cons high-card-action
          (case (:card/face card)
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
  (vec (set (map #(select-keys % [:card/face :card/value]) hand))))

(def personality-profiles
  {:aggressive {:eliminate 0.80
                :assist    0.70
                :high-card 0.40
                :defensive 0.40
                :survive   0.25
                :bluff     0.15
                :suicide   1}

   :defensive {:eliminate 0.40
               :assist    0.25
               :high-card 0.40
               :defensive 0.80
               :survive   0.80
               :bluff     0.15
               :suicide   1}

   :base {:eliminate 1
          :assist    1
          :high-card 1
          :defensive 1
          :survive   1
          :bluff     1
          :suicide   1}})

(defn apply-personality
  "Applies a given personality to the action."
  [personality action]
  (let [type (:type action)]
    (update action :strength * (type (personality personality-profiles)))))

(defn generate-actions [game player-id]
  (let [personality (get-in game [:game/players player-id :personality])]
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

(defn no-op-action
  "Used to handle discarding a card without resolving it's effects."
  [face current-player]
  {:action {:active-card face
            :card-target nil
            :game/current-player current-player
            :guard-guess nil}})

(defn pick-action
  "A pretty ugly function that is used handle action picking
   or no-op when there is no appropriate action."
  [actions]
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


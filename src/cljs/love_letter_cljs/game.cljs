(ns love-letter-cljs.game
  "Main game logic, game generation and rules for each individual card"
  (:require [clojure.set :as set]
            [love-letter-cljs.utils :as utils]]))

(def cards [{:face :guard    :value 1 :count 5}
            {:face :priest   :value 2 :count 2}
            {:face :baron    :value 3 :count 2}
            {:face :handmaid :value 4 :count 2}
            {:face :prince   :value 5 :count 2}
            {:face :king     :value 6 :count 1}
            {:face :countess :value 7 :count 1}
            {:face :princess :value 8 :count 1}])

(defn generate-card [card]
  (let [{:keys [count face value]} card]
    (repeat count {:face face :value value :visible []})))

(defn generate-deck []
  (-> (mapcat generate-card cards) shuffle vec))

(defn create-player [n personality]
  {:id          n
   :hand        []
   :alive?      true
   :protected?  false
   :personality personality})

(def personalities
  [:aggressive :base :defensive])

(defn create-players [n]
  (mapv #(create-player %1 (rand-nth personalities)) (range 1 (inc n))))

(defn add-players [n]
  (reduce
   (fn [m player]
     (assoc m (:id player) player)) {} (create-players n)))

(defn create-game []
  {:deck           (generate-deck)
   :discard-pile   []
   :burn-pile      []
   :players        (add-players 4)
   :current-player 1})

(defn move-card [game source destination]
  "Move the first card from a source to a destination"
  (let [card (first (get-in game source))]
    (-> game
        (update-in source (comp vec rest))
        (update-in destination conj card))))

(defn deal-cards [game]
  (let [player-ids (keys (:players game))]
    (reduce
     (fn [g id]
       (move-card g [:deck] [:players id :hand])) game player-ids)))

(defn create-and-deal []
  (-> (create-game)
      (move-card [:deck] [:burn-pile])
      (deal-cards)))

(defn kill-player
  "Deactivates a player and moves their card to the discard pile"
  [game target]
  (-> game
      (update-in [:players target :alive?] not)
      (move-card [:players target :hand] [:discard-pile])))

(defn reveal-card-to-player [game player target]
  "Adds a target's id to the given player's card visibility list"
  (update-in game [:players target :hand 0 :visible] conj player))

(defn handmaid-ability [game player]
  "Protects the player from card effects"
  (assoc-in game [:players player :protected?] true))

(defn baron-ability [game player target]
  "Compares the player's card to target, lower value is removed
   from game"
  (let [player-card (utils/find-card game player)
        target-card (utils/find-card game target)]
    (condp #(%1 (:value player-card) %2) (:value target-card)
      > (kill-player game target)
      < (kill-player game player)
      = (-> game
            (reveal-card-to-player player target)
            (reveal-card-to-player target player)))))

(defn guard-ability [game guess target]
  "Guesses a targets card, removes the player if the guess
   is correct"
  (let [target-card-face (:face (utils/find-card game target))]
    (if (and (not= :guard target-card-face)
             (= guess target-card-face))
      (kill-player game target)
      game)))

(defn king-ability [game player target]
  "Trades the player's card with the target's card"
  (let [player-card (utils/find-card game player)
        target-card (utils/find-card game target)]
    (-> game
        (assoc-in [:players target :hand] [(update-in player-card [:visible] conj player)])
        (assoc-in [:players player :hand] [(update-in target-card [:visible] conj target)]))))

(defn prince-ability [game target]
  "Causes the target to discard their card"
  (let [target-card-face (:face (utils/find-card game target))]
    (if (= :princess target-card-face)
      (kill-player game target)
      (-> game
          (move-card [:players target :hand] [:discard-pile])
          (move-card [:deck] [:players target :hand])))))

(defn score-hand [player]
  (let [hand (player :hand)]
    (merge
     (select-keys player [:id])
     (select-keys (peek hand) [:face :value]))))

(defn score-game [game]
  (-> game
      :players
      vals
      (->>
       (filter :alive?)
       (mapv score-hand))))

(defn determine-winner [game]
  (->> game
       score-game
       (apply max-key :value)))

(defn count-alive [game]
  "Returns the number of players remaining"
  (-> game
      :players
      vals
      (->>
       (filter :alive?)
       count)))

(defn game-complete? [game]
  "Game is complete when one player remains or the deck is empty"
  (or (= 1 (count-alive game))
      (empty? (:deck game))))

(defn countess-check [game player-id]
  "Checks the players hand for cards that cause the countess to
   be discarded automatically"
  (->> (get-in game [:players player-id :hand])
       (map :face)
       set
       (some #{:prince :king})
       ((complement nil?))))

(defn remove-protection [game]
  (assoc-in game [:players (:current-player game) :protected?] false))

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

(defn move-card
  "Move the first card from a source to a destination"
  [game source destination]
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
      (assoc-in [:players target :alive?] false)
      (move-card [:players target :hand] [:discard-pile])))

(defn reveal-card-to-player
  "Adds a target's id to the given player's card visibility list"
  [game player target]
  (update-in game [:players target :hand 0 :visible] conj player))

(defn handmaid-ability
  "Protects the player from card effects"
  [game player]
  (assoc-in game [:players player :protected?] true))

(defn baron-ability
  "Compares the player's card to target, lower value is removed from game"
  [game player target]
  (let [player-card (utils/find-card game player)
        target-card (utils/find-card game target)]
    (condp #(%1 (:value player-card) %2) (:value target-card)
      > (kill-player game target)
      < (kill-player game player)
      = (-> game
            (reveal-card-to-player player target)
            (reveal-card-to-player target player)))))

(defn guard-ability
  "Guesses a targets card, removes the player if the guess is correct"
  [game guess target]
  (let [target-card-face (:face (utils/find-card game target))]
    (if (and (not= :guard target-card-face)
             (= guess target-card-face))
      (kill-player game target)
      game)))

(defn king-ability
  "Trades the player's card with the target's card"
  [game player target]
  (let [player-card (utils/find-card game player)
        target-card (utils/find-card game target)]
    (-> game
        (assoc-in [:players target :hand] [(update-in player-card [:visible] conj player)])
        (assoc-in [:players player :hand] [(update-in target-card [:visible] conj target)]))))

(defn prince-ability
  "Causes the target to discard their card"
  [game target]
  (let [target-card-face (:face (utils/find-card game target))]
    (if (= :princess target-card-face)
      (kill-player game target)
      (-> game
          (move-card [:players target :hand] [:discard-pile])
          (move-card [:deck] [:players target :hand])))))

(defn score-hand [player]
  (let [player-card (-> player :hand peek)]
    (merge
     (select-keys player [:id])
     (select-keys player-card [:face :value]))))

(defn start-next-turn [game]
  (let [current-player (:current-player game)
        players        (player-list game)
        next-player    (next-in-list players current-player)]
    (if (game-complete? game)
      (-> (set-phase game :complete)
          (assoc :active-screen :win-screen))
      (-> (assoc game
                 :current-player next-player
                 :active-card nil
                 :card-target nil
                 :guard-guess nil)
          (assoc-in [:players next-player :protected?] false)
          (set-phase :draw)))))

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

(defn count-alive
  "Returns the number of players remaining"
  [game]
  (-> game
      :players
      vals
      (->>
       (filter :alive?)
       count)))

(defn game-complete?
  "Game is complete when one player remains or the deck is empty"
  [game]
  (or (= 1 (count-alive game))
      (empty? (:deck game))))

(defn countess-check
  "Checks the players hand for cards that cause the countess to
   be discarded automatically"
  [game player-id]
  (->> (get-in game [:players player-id :hand])
       (map :face)
       set
       (some #{:prince :king})
       ((complement nil?))))

(defn remove-protection [game]
  (assoc-in game [:players (:current-player game) :protected?] false))

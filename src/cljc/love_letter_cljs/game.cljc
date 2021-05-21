(ns love-letter-cljs.game
  "Main game logic, game generation and rules for each individual card"
  (:require [clojure.set :as set]
            [clojure.spec.alpha :as s]
            [love-letter-cljs.ai :as ai]
            [love-letter-cljs.utils :as utils]))

(def cards [{:card/face :guard    :card/value 1 :card/count 5}
            {:card/face :priest   :card/value 2 :card/count 2}
            {:card/face :baron    :card/value 3 :card/count 2}
            {:card/face :handmaid :card/value 4 :card/count 2}
            {:card/face :prince   :card/value 5 :card/count 2}
            {:card/face :king     :card/value 6 :card/count 1}
            {:card/face :countess :card/value 7 :card/count 1}
            {:card/face :princess :card/value 8 :card/count 1}])

(s/def :card/face #{:guard :priest :baron :handmaid :prince :king :countess :princess})
(s/def :card/value int?)
(s/def :card/visible (s/coll-of int?)) ;; Player Ids
(s/def ::card (s/keys :req [:card/face :card/value :card/visible]))

(s/def :player/id int?)

(def card? (partial s/valid? ::card))

(s/def :player/hand (partial every? card?))
;; TODO: player STATE | (alive, dead, protected)
(s/def :player/alive? boolean?)
(s/def :player/protected? boolean?)
(s/def :player/personality (set ai/personalities))

(s/def ::player (s/keys :req [:player/id :player/alive? :player/hand :player/protected? :player/personality]))

(def player? (partial s/valid? ::player))
(s/def :game/deck (partial every? card?))
(s/def :game/discard-pile (partial every? card?))
(s/def :game/burn-pile (partial every? card?))
(s/def :game/players (partial every? player?))
(s/def :game/current-player int?)
(s/def ::game (s/keys :req [:game/deck
                            :game/discard-pile
                            :game/burn-pile
                            :game/players
                            :game/current-player]))

(defn generate-card [card]
  (let [{:card/keys [count face value]} card]
    (repeat count {:card/face face :card/value value :card/visible []})))

(defn generate-deck []
  (-> (mapcat generate-card cards) shuffle vec))

(defn create-player [n personality]
  {:player/id          n
   :player/hand        []
   :player/alive?      true
   :player/protected?  false
   :player/personality personality})

(defn create-players [n]
  (mapv #(create-player %1 (rand-nth ai/personalities)) (range 1 (inc n))))

(defn add-players [n]
  (reduce
   (fn [m player]
     (assoc m (:player/id player) player)) {} (create-players n)))

(defn create-game
  ([]
   (create-game (generate-deck) (add-players 4)))
  ([deck players]
   {:game/deck           deck
    :game/discard-pile   []
    :game/burn-pile      []
    :game/players        players
    :game/current-player 1}))

(defn move-card
  "Move the first card from a source to a destination"
  [game source destination]
  (let [card (first (get-in game source))]
    (-> game
        (update-in source (comp vec rest))
        (update-in destination conj card))))

(defn deal-cards [game]
  (let [player-ids (keys (:game/players game))]
    (reduce
     (fn [g id]
       (move-card g [:game/deck] [:game/players id :player/hand])) game player-ids)))

(defn create-and-deal []
  (-> (create-game)
      (move-card [:game/deck] [:game/burn-pile])
      deal-cards))

(defn kill-player
  "Deactivates a player and moves their card to the discard pile"
  [game target]
  (-> game
      (assoc-in [:game/players target :player/alive?] false)
      (move-card [:game/players target :player/hand] [:game/discard-pile])))

(defn reveal-card-to-player
  "Adds a target's id to the given player's card visibility list"
  [game player target]
  (update-in game [:game/players target :player/hand 0 :card/visible] conj player))

(defn handmaid-ability
  "Protects the player from card effects"
  [game player]
  (assoc-in game [:game/players player :player/protected?] true))

(defn baron-ability
  "Compares the player's card to target, lower value is removed from game"
  [game player target]
  (let [player-card (utils/find-card game player)
        target-card (utils/find-card game target)]
    (condp #(%1 (:card/value player-card) %2) (:card/value target-card)
      > (kill-player game target)
      < (kill-player game player)
      = (-> game
            (reveal-card-to-player player target)
            (reveal-card-to-player target player)))))

(defn guard-ability
  "Guesses a targets card, removes the player if the guess is correct"
  [game guess target]
  (let [target-card-face (:card/face (utils/find-card game target))]
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
        (assoc-in [:game/players target :player/hand] [(update-in player-card [:card/visible] conj player)])
        (assoc-in [:game/players player :player/hand] [(update-in target-card [:card/visible] conj target)]))))

(defn prince-ability
  "Causes the target to discard their card"
  [game target]
  (let [target-card-face (:card/face (utils/find-card game target))]
    (if (= :princess target-card-face)
      (kill-player game target)
      (-> game
          (move-card [:game/players target :player/hand] [:game/discard-pile])
          (move-card [:game/deck] [:game/players target :player/hand])))))

(defn score-hand [player]
  (let [player-card (-> player :player/hand peek)]
    (merge
     (select-keys player [:player/id])
     (select-keys player-card [:card/face :card/value]))))

(defn count-alive
  "Returns the number of players remaining"
  [game]
  (->> game
       :game/players
       vals
       (filter :player/alive?)
       count))

(defn game-complete?
  "Game is complete when one player remains or the deck is empty"
  [game]
  (or (= 1 (count-alive game))
      (empty? (:game/deck game))))

(defn start-next-turn [game]
  (let [current-player (:game/current-player game)
        players        (utils/player-list game)
        next-player    (utils/next-in-list players current-player)]
    (if (game-complete? game)
      (-> (utils/set-phase game :complete)
          (assoc :active-screen :win-screen))
      (-> (assoc game
                 :game/current-player next-player
                 :active-card nil
                 :card-target nil
                 :guard-guess nil)
          (assoc-in [:game/players next-player :player/protected?] false)
          (utils/set-phase :draw)))))

(defn score-game [game]
  (->> game
       :game/players
       vals
       (filter :player/alive?)
       (mapv score-hand)))

(defn determine-winner [game]
  (->> game
       score-game
       (apply max-key :card/value)))

(defn countess-check
  "Checks the players hand for cards that cause the countess to
   be discarded automatically"
  [game player-id]
  (->> (get-in game [:game/players player-id :player/hand])
       (map :card/face)
       set
       (some #{:prince :king})
       ((complement nil?))))

(defn remove-protection [game]
  (assoc-in game [:game/players (:game/current-player game) :player/protected?] false))

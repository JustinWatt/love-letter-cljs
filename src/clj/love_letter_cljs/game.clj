(ns love-letter-cljs.game
  (:require [clojure.set :as set]))

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
    (repeat count {:face face :value value})))

(defn generate-deck []
  (vec (shuffle (mapcat generate-card cards))))

(defn create-player [n]
  {:id n
   :hand []
   :alive? true})

(defn create-players [n]
  (for [p (range 1 (inc n))]
    (create-player p)))

(defn add-players [n]
  (reduce
   (fn [m player]
     (assoc m (:id player) player)) {} (create-players n)))

(defn create-game []
  {:deck (generate-deck)
   :discard-pile []
   :players (add-players 4)
   :current-player 1})

(defn draw-card [game player]
  (let [deck (:deck game)]
    (-> game
        (update-in [:players player :hand] into (take 1 deck))
        (assoc-in [:deck] (drop 1 deck)))))

(defn deal-cards [game]
  (let [players (keys (game :players))]
    (loop [game    game
           players players]
      (if (empty? players)
        game
        (recur (draw-card game (first players))
               (rest players))))))

(defn discard-card [game source]
  (let [cards (get-in game source)]
    (-> game
        (update-in [:discard-pile] into (vec (take 1 cards)))
        (assoc-in source (drop 1 cards)))))

(defn reveal-card [game target]
  (-> game
      (get-in [:players target :hand])
      first))

(defn create-and-deal []
  (-> (create-game)
      (deal-cards)))

(defn- kill-player
  [game target]
  (update-in game [:players target :alive?] not))

(defn baron-ability [game player target]
  (let [player-card (reveal-card game player)
        target-card (reveal-card game target)]
    (condp #(%1 (:value player-card) %2) (:value target-card)
        > (kill-player game target)
        < (kill-player game player)
        = game)))

(defn guard-ability [game guess target]
  (let [target-card (reveal-card game target)]
    (if (and (not= :guard (target-card :face))
             (= guess (target-card :face)))
             (kill-player game target)
             game)))

(defn king-ability [game player target]
  (let [player-card (reveal-card game player)
        target-card (reveal-card game target)]
    (-> game
        (assoc-in [:players player :hand] target-card)
        (assoc-in [:players target :hand] player-card))))

(defn prince-ability [game target]
  (-> game
      (discard-card [:player target :hand])
      (draw-card target)))

(defn handmaid-ability [game player]
  (-> game
      (assoc-in [:players player :protected?] true)))

(defn score-hand [player]
  (let [hand (player :hand)]
    (merge
     (select-keys player [:id])
     (select-keys (first hand) [:face :value]))))

(defn score-game [game]
  (-> game
      (get-in [:players])
      vals
      (->>
       (filter :alive?)
       (mapv score-hand))))

(defn determine-winner [game]
  (->> game
       score-game
       (apply max-key :value)))

(defn count-alive [game]
  (-> game
      (get-in [:players])
      vals
      (->>
       (filter :alive?)
       count)))

(defn game-complete? [game]
  (or (= 1 (count-alive game))
      (empty? (:deck game))))

(defn countess-check [game player]
  (let [hand (get-in game [:players player :hand])]
    (->> hand
         (map :face)
         set
         (set/intersection #{:prince :king :princess})
         empty?
         not)))

(defn- valid-target? [current-player player]
  (and (not= current-player (:id player))
       (or (:protected? player)
           (:alive? player))))

(defn valid-targets [game]
  (let [current-player (:current-player game)]
    (-> game
        :players
        vals
        (->>
         (filter #(valid-target? current-player %))
         (map :id)))))

(defn card-action [game face]
  (let [current-player (:current-player game)]
    (face
     {:guard     (partial guard-ability    game)
      :priest    (partial reveal-card      game)
      :baron     (partial baron-ability    game current-player)
      :handmaid  (partial handmaid-ability game current-player)
      :prince    (partial prince-ability   game current-player)
      :king      (partial king-ability     game)})))

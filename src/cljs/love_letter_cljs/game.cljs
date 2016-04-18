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

(defn remove-first [face coll]
  (let [[pre post] (split-with #(not= face (:face %)) coll)]
    (vec (concat pre (rest post)))))

(defn generate-card [card]
  (let [{:keys [count face value]} card]
    (repeat count {:face face :value value :visible []})))

(defn generate-deck []
  (-> (mapcat generate-card cards) shuffle vec))

(defn create-player [n]
  {:id n
   :hand []
   :alive? true
   :protected? false})

(defn create-players [n]
  (mapv create-player (range 1 (inc n))))

(defn add-players [n]
  (reduce
   (fn [m player]
     (assoc m (:id player) player)) {} (create-players n)))

(defn create-game []
  {:deck (generate-deck)
   :discard-pile []
   :burn-pile []
   :players (add-players 4)
   :current-player 1})

(defn draw-card [game player-id]
  (let [deck (:deck game)]
    (-> game
        (update-in [:players player-id :hand] into (take 1 deck))
        (assoc :deck (drop 1 deck)))))

(defn deal-cards [game]
  (let [player-ids (keys (:players game))]
    (reduce draw-card game player-ids)))

(defn burn-card [game]
  (let [cards (:deck game)]
    (-> game
        (update-in [:burn-pile] into (vec (take 1 cards)))
        (assoc :deck (drop 1 cards)))))

(defn discard-card [game source]
  (let [cards (get-in game source)]
    (-> game
        (update-in [:discard-pile] into (vec (take 1 cards)))
        (assoc-in source (drop 1 cards)))))

(defn find-card [game target]
  (-> game
      (get-in [:players target :hand])
      peek))

(defn create-and-deal []
  (-> (create-game)
      (burn-card)
      (deal-cards)))

(defn- kill-player
  [game target]
  (-> game
      (update-in [:players target :alive?] not)
      (discard-card [:players target :hand])))

(defn reveal-card-to-player [game player target]
  (update-in game [:players target :hand 0 :visible] conj player))

(defn handmaid-ability [game player]
  (assoc-in game [:players player :protected?] true))

(defn baron-ability [game player target]
  (let [player-card (find-card game player)
        target-card (find-card game target)]
    (condp #(%1 (:value player-card) %2) (:value target-card)
      > (kill-player game target)
      < (kill-player game player)
      = (-> game
            (reveal-card-to-player player target)
            (reveal-card-to-player target player)))))

(defn guard-ability [game guess target]
  (let [target-card (find-card game target)]
    (if (and (not= :guard (target-card :face))
             (= guess (target-card :face)))
      (kill-player game target)
      game)))

(defn king-ability [game player target]
  (let [player-card (find-card game player)
        target-card (find-card game target)]
    (-> game
        (assoc-in [:players target :hand] [(update-in player-card [:visible] conj [player])])
        (assoc-in [:players player :hand] [(update-in target-card [:visible] conj [target])]))))


(defn prince-ability [game target]
  (let [target-card (find-card game target)]
    (if (= :princess target-card)
      (kill-player game target)
      (-> game
          (discard-card [:players target :hand])
          (draw-card target)))))

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
  (-> game
      :players
      vals
      (->>
       (filter :alive?)
       count)))

(defn game-complete? [game]
  (or (= 1 (count-alive game))
      (empty? (:deck game))))

(defn countess-check [game player-id]
  (->> (get-in game [:players player-id :hand])
       (map :face)
       set
       (some #{:prince :king})
       ((complement nil?))))

(defn- valid-target? [current-player player]
  (and (not= current-player (:id player))
       (and (not (:protected? player))
            (:alive? player))))

(defn valid-targets [game]
  (let [current-player (:current-player game)]
    (-> game
        :players
        vals
        (->>
         (filter (partial valid-target? current-player))
         (map :id)))))

(defn remove-protection [game]
  (assoc-in game [:players (:current-player game) :protected?] false))


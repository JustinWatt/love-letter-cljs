(ns love-letter-cljs.handlers
    (:require [re-frame.core :refer [dispatch register-handler]]
              [love-letter-cljs.db :as db]
              [love-letter-cljs.game :as l]))

(register-handler
 :initialize-db
 (fn  [_ _]
   db/default-db))

(def reset-state
  {:display-card nil
   :phase :draw
   :active-card nil
   :guard-guess nil
   :card-target nil})

(register-handler
 :new-game
 (fn [db _]
   (-> db
       (assoc-in [:state] reset-state)
       (assoc-in [:game]  (l/create-and-deal)))))

(register-handler
 :reset-state
 (fn [db _]
    (assoc-in db [:state] reset-state)))

(register-handler
 :set-display-card
 (fn [db [_ face]]
   (assoc-in db [:state :display-card] face)))

(register-handler
 :draw-card
 (fn [db [_ player-id]]
   (assoc-in db [:game] (l/draw-card (:game db) player-id))))

(defn set-phase [db phase]
  (assoc-in db [:state :phase] phase))

(register-handler
 :set-phase
 (fn [db [_ phase]]
   (set-phase db phase)))

(register-handler
 :set-active-card
 (fn [db [_ face]]
   (as-> db d
     (assoc-in d [:state :active-card] face)
     (condp = face
       :princess (set-phase d :resolution)
       :guard    (set-phase d :guard)
       (set-phase d :target)))))

(register-handler
 :set-target
 (fn [db [_ target-id]]
   (let [active-card (:state (:active-card db))]
     (as-> db d
       (assoc-in d [:state :card-target] target-id)
       (condp = active-card
         :guard (set-phase d :guard)
         (set-phase d :resolution))))))

(register-handler
 :set-guard-guess
 (fn [db [_ face]]
   (as-> db d
     (assoc-in d [:state :guard-guess] face)
     (condp = face
       :guard (set-phase d :guard)
       (set-phase d :resolution)))))

;; For cycling turns
(defn next-in-list [current item-list]
  (as-> item-list i
    (drop-while #(not= current %) i)
    (or (first (next i)) (first item-list))))

(defn player-list [game]
  (->> game
       :players
       vals
       (filter :alive?)
       (mapv :id)))

(register-handler
 :next-player
 re-frame.core/debug
 (fn [db _]
   (let [current-player (:current-player (:game db))
         players        (player-list (:game db))
         next-player    (next-in-list current-player players)]
     (-> db
         (assoc-in [:game :current-player] next-player)
         (assoc-in [:game :players next-player :protected?] false)))))

(def card-abilities
  {:guard    l/guard-ability
   :baron    l/baron-ability
   :handmaid l/handmaid-ability
   :prince   l/prince-ability
   :king     l/king-ability})

(defn remove-first [face coll]
  (let [[pre post] (split-with #(not= face (:face %)) coll)]
    (vec (concat pre (rest post)))))

(defn play-card [db face current-player]
  (let [path [:game :players current-player :hand]]
    (assoc-in db path (remove-first face (get-in db path)))))

(defn resolve-effect [db]
  (let [{:keys [card-target active-card guard-guess]} (:state db)
        game (:game db)
        current-player (:current-player game)
        card-effect    (active-card card-abilities)]
    (condp = active-card
      :prince (assoc db :game (card-effect game card-target))
      :guard  (assoc db :game (card-effect game guard-guess card-target))
      :baron  (assoc db :game (card-effect game current-player card-target))
      :king   (assoc db :game (card-effect game current-player card-target))
      :handmaid (assoc db :game (card-effect game current-player))
      :countess db
      :priest   db
      :princess (assoc db :game (l/kill-player game current-player))
      (assoc db :game (card-effect game current-player card-target)))))

(register-handler
 :resolve-effect
 re-frame.core/debug
 (fn [db _]
   (let [active-card    (get-in db [:state :active-card])
         current-player (get-in db [:game  :current-player])]
     (-> db
         (play-card active-card current-player)
         (resolve-effect)))))


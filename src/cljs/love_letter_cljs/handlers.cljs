(ns love-letter-cljs.handlers
    (:require [re-frame.core :refer [dispatch register-handler]]
              [love-letter-cljs.db :as db]
              [love-letter-cljs.game :as g]))

(register-handler
 :initialize-db
 (fn  [_ _]
   db/default-db))

(defn reset-state [db]
  (update-in db [:state] merge {:display-card nil
                                :phase :draw
                                :active-card nil
                                :guard-guess nil
                                :card-target nil
                                :log []}))

(register-handler
 :new-game
 (fn [db _]
   (-> db
       (reset-state)
       (update-in [:game] merge (g/create-and-deal)))))

(register-handler
 :set-display-card
 (fn [db [_ face]]
   (assoc-in db [:state :display-card] face)))

(defn set-phase [db phase]
  (assoc-in db [:state :phase] phase))

(register-handler
 :set-phase
 (fn [db [_ phase]]
   (set-phase db phase)))

(defn transition-from-play-phase [db face]
  (case face
    :princess (set-phase db :resolution)
    :handmaid (set-phase db :resolution)
    :countess (set-phase db :resolution)
    (set-phase db :target)))

(register-handler
 :set-active-card
 (fn [db [_ face]]
   (-> db
       (assoc-in [:state :active-card] face)
       (transition-from-play-phase face))))

(defn transition-from-target-phase [db face]
  (if (= :guard face)
    (set-phase db :guard)
    (set-phase db :resolution)))

(register-handler
 :set-target
 (fn [db [_ target-id]]
   (let [active-card (-> db :state :active-card)]
     (-> db
         (assoc-in [:state :card-target] target-id)
         (transition-from-target-phase active-card)))))

(register-handler
 :set-guard-guess
 (fn [db [_ face]]
   (-> db
       (assoc-in [:state :guard-guess] face)
       (set-phase :resolution))))

;; For cycling turns
(defn next-in-list [item-list current]
  (as-> item-list i
    (drop-while #(not= current %) i)
    (or (first (next i))
        (first item-list))))

(defn player-list [game]
  (->> game
       :players
       vals
       (filter :alive?)
       (mapv :id)))

(defn start-next-turn [db]
  (let [current-player (:current-player (:game db))
        players        (player-list (:game db))
        next-player    (next-in-list players current-player)]
    (if (g/game-complete? (:game db))
      (set-phase db :complete)
      (-> db
          (assoc-in [:game :current-player] next-player)
          (assoc-in [:game :players next-player :protected?] false)
          (set-phase :draw)))))

(register-handler
 :next-player
 (fn [db _]
   (start-next-turn db)))

(defn retrieve-card [db face current-player]
  (let [path [:game :players current-player :hand]]
    (->> (get-in db path)
         (filter #(= face (:face %)))
         first)))

(defn play-card [db face player-id]
  (let [path [:game :players player-id :hand]
        discarded-card (retrieve-card db face player-id)
        discard-pile (get-in db [:game :discard-pile])]
    (-> db
        (assoc-in path (g/remove-first face (get-in db path)))
        (update-in [:game :discard-pile] conj discarded-card))))

(register-handler
 :draw-card
 (fn [db [_ player-id]]
   (as-> db d
     (assoc-in d [:game] (g/draw-card (:game db) player-id))
     #_(if (g/countess-check (:game d) player-id)
       (-> d
           (play-card :countess player-id)
           (start-next-turn))
       d))))

(defn update-game [db game]
  (assoc db :game game))

(defn resolve-effect [db]
  (let [{:keys [card-target active-card guard-guess]} (:state db)
        game (:game db)
        current-player (:current-player game)]
    (case active-card
      :prince   (update-game db (g/prince-ability   game card-target))
      :guard    (update-game db (g/guard-ability    game guard-guess card-target))
      :baron    (update-game db (g/baron-ability    game current-player card-target))
      :king     (update-game db (g/king-ability     game current-player card-target))
      :handmaid (update-game db (g/handmaid-ability game current-player))
      :countess db
      :priest   (update-game db (g/reveal-card-to-player game current-player card-target))
      :princess (update-game db (g/kill-player game current-player))
      :default db)))

(register-handler
 :resolve-effect
 (fn [db _]
   (let [active-card    (get-in db [:state :active-card])
         current-player (get-in db[:game  :current-player])]
     (-> db
         (play-card active-card current-player)
         (resolve-effect)
         (start-next-turn)))))

(register-handler
 :discard-without-effect
 (fn [db]
   (let [active-card    (get-in db [:state :active-card])
         current-player (get-in db [:game  :current-player])]
     (-> db
         (play-card active-card current-player)
         (start-next-turn)))))

(register-handler
 :toggle-debug-mode
 (fn [db]
   (update-in db [:state :debug-mode?] not)))

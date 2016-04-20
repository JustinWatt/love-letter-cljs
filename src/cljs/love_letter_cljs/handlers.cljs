(ns love-letter-cljs.handlers
    (:require [re-frame.core :refer [after debug undoable dispatch register-handler]]
              [love-letter-cljs.db :as db]
              [love-letter-cljs.utils :as u]
              [love-letter-cljs.game :as g]
              [cljs.core.match :refer-macros [match]]))

(def standard-middlewares [(when ^boolean goog.DEBUG debug)
                           (when ^boolean goog.DEBUG (after db/valid-schema?))]) 

(register-handler
 :initialize-db
 (fn  [_ _]
   db/default-db))

(defn reset-state [db]
  (merge db {:display-card nil
             :phase :draw
             :active-card nil
             :guard-guess nil
             :card-target nil
             :log []}))

(register-handler
 :new-game
 [(undoable) standard-middlewares]
 (fn [db _]
   (-> db
       (reset-state)
       (merge (g/create-and-deal)))))

(register-handler
 :set-display-card
 standard-middlewares
 (fn [db [_ face]]
   (assoc-in db [:display-card] face)))

(defn set-phase [db phase]
  (assoc-in db [:phase] phase))

(defn transition-phase [db from face]
  (match [from face]
    [:draw _]         (set-phase db :play)
    [:play :princess] (set-phase db :resolution)
    [:play :handmaid] (set-phase db :resolution)
    [:play :countess] (set-phase db :resolution)
    [:play _]         (set-phase db :target)
    [:target :guard]  (set-phase db :guard)
    [:target _]       (set-phase db :resolution)
    [:guard _]        (set-phase db :resolution)
    [:resolution _]   (set-phase db :draw)))

(register-handler
 :set-active-card
 [(undoable)
 standard-middlewares]
 (fn [db [_ face]]
   (-> db
       (assoc-in [:active-card] face)
       (transition-phase :play face))))

(defn transition-from-target-phase [db face]
  (if (= :guard face)
    (set-phase db :guard)
    (set-phase db :resolution)))

(register-handler
 :set-target
 [(undoable)
 standard-middlewares]
 (fn [db [_ target-id]]
   (let [active-card (:active-card db)]
     (-> db
         (assoc-in [:card-target] target-id)
         (transition-phase :target active-card)))))

(register-handler
 :set-guard-guess
 [(undoable)
       standard-middlewares]
 (fn [db [_ face]]
   (-> db
       (assoc-in [:guard-guess] face)
       (transition-phase :guard _))))

;; For cycling turns
(defn next-in-list [item-list current]
  (as-> item-list i
    (drop-while #(< current %) i)
    (or (first (next i))
        (first item-list))))

(defn player-list [game]
  (->> game
       :players
       vals
       (filter :alive?)
       (mapv :id)))

(defn start-next-turn [db]
  (let [current-player (:current-player db)
        players        (player-list db)
        next-player    (next-in-list players current-player)]
    (if (g/game-complete? db)
      (set-phase db :complete)
      (-> db
          (assoc-in [:current-player] next-player)
          (assoc-in [:players next-player :protected?] false)
          (set-phase :draw)))))

(defn retrieve-card [db face current-player]
  (let [path [:players current-player :hand]]
    (->> (get-in db path)
         (filter #(= face (:face %)))
         first)))

(defn play-card [db face player-id]
  (let [path [:players player-id :hand]
        discarded-card (retrieve-card db face player-id)]
    (when (nil? discarded-card) (throw (js/Error. "Tried to discard a nil card")))
    (-> db
        (assoc-in path (u/remove-first face (get-in db path)))
        (update-in [:discard-pile] conj discarded-card))))

(defn handle-draw-card [db player-id]
  (let [with-card-drawn (g/move-card db [:deck] [:players player-id :hand])]
    #_(if (g/countess-check with-card-drawn player-id)
      (-> with-card-drawn
          (play-card :countess player-id)
          (start-next-turn)))
      (set-phase with-card-drawn :play)))

(register-handler
 :draw-card
 [(undoable)
 standard-middlewares]
 (fn [db [_ player-id]]
   (handle-draw-card db player-id)))

(defn resolve-effect [db]
  (let [{:keys [card-target active-card guard-guess]} db
        game db
        current-player (:current-player game)]
    (case active-card
      :prince   (merge db (g/prince-ability   db card-target))
      :guard    (merge db (g/guard-ability    db guard-guess card-target))
      :baron    (merge db (g/baron-ability    db current-player card-target))
      :king     (merge db (g/king-ability     db current-player card-target))
      :handmaid (merge db (g/handmaid-ability db current-player))
      :countess db
      :priest   (merge db (g/reveal-card-to-player db current-player card-target))
      :princess (merge db (g/kill-player db current-player))
      :default db)))

(register-handler
 :resolve-effect
 [(undoable)
 standard-middlewares]
 (fn [db _]
   (let [active-card    (:active-card db)
         current-player (:current-player db)]
     (-> db
         (play-card active-card current-player)
         (resolve-effect)
         (start-next-turn)))))

(register-handler
 :discard-without-effect
 [(undoable)
 standard-middlewares]
 (fn [db]
   (let [active-card    (:active-card db)
         current-player (:current-player db)]
     (-> db
         (play-card active-card current-player)
         (start-next-turn)))))

(register-handler
 :toggle-debug-mode
 standard-middlewares
 (fn [db]
   (update db :debug-mode? not)))


(register-handler
 :load-game
 standard-middlewares
 (fn [db [_ game]]
   (merge db game)))


(def a
  {:deck '({:face :prince :value 5 :visible []}
          {:face :guard :value 1 :visible []}
          {:face :baron :value 3 :visible []}
          {:face :guard :value 1 :visible []}
          {:face :king :value 6 :visible []})
   :debug-mode? false
   :display-card nil
   :phase :resolution
   :discard-pile [{:face :countess :value 7 :visible []}
                  {:face :handmaid :value 4 :visible []}
                  {:face :guard :value 1 :visible []}
                  {:face :prince :value 5 :visible []}
                  {:face :baron :value 3 :visible []}]

   :burn-pile [{:face :handmaid :value 4 :visible []}]
   :card-target 3
   :guard-guess :priest
   :active-card :priest
   :players {1 {:id 1 :hand [{:face :princess :value 8 :visible []}
                              {:face :priest :value 2 :visible []}]
                :alive? true :protected? false}
             2 {:id 2 :hand [{:face :guard :value 1 :visible []}]
                :alive? true :protected? true}
             3 {:id 3 :hand '({:face :priest :value 2 :visible []})
                :alive? true :protected? false}
             4 {:id 4 :hand [{:face :guard :value 1 :visible []}]
                :alive? true :protected? false}}
   :log []
   :current-player 2})

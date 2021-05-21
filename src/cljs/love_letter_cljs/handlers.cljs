(ns love-letter-cljs.handlers
  (:require [re-frame.core :refer [trim-v after debug undoable dispatch register-handler]]
            [love-letter-cljs.db :as db]
            [love-letter-cljs.utils :as u]
            [love-letter-cljs.game :as g]
            [love-letter-cljs.ai :as ai]
            [cljs.core.match :refer-macros [match]]
            [clojure.string :as s]
            [love-letter-cljs.utils :as utils]))

(def standard-middleware [(when ^boolean goog.DEBUG debug)
                          (when ^boolean goog.DEBUG (after db/valid-schema?))
                          trim-v])

(register-handler
 :initialize-db
 (fn  [_ _]
   db/default-db))

(register-handler
 :set-active-screen
 standard-middleware
 (fn [db [screen-key]]
   (assoc db :active-screen screen-key)))

(defn reset-state [db]
  (merge db {:display-card nil
             :phase :draw
             :active-card nil
             :guard-guess nil
             :card-target nil
             :log []}))

(register-handler
 :new-game
 [(undoable) standard-middleware]
 (fn [db _]
   (-> db
       reset-state
       (merge (g/create-and-deal)))))

(register-handler
 :set-display-card
 standard-middleware
 (fn [db [_ face]]
   (assoc-in db [:display-card] face)))

(defn- transition-phase [db from face]
  (match [from face]
    [:draw _]         (utils/set-phase db :play)
    [:play :princess] (utils/set-phase db :resolution)
    [:play :handmaid] (utils/set-phase db :resolution)
    [:play :countess] (utils/set-phase db :resolution)
    [:play _]         (utils/set-phase db :target)
    [:target :guard]  (utils/set-phase db :guard)
    [:target _]       (utils/set-phase db :resolution)
    [:guard _]        (utils/set-phase db :resolution)
    [:resolution _]   (utils/set-phase db :draw)))

(defn set-active-card-handler [db [face]]
  (-> db
      (assoc-in [:active-card] face)
      (assoc :card-target nil)
      (assoc :guard-guess nil)
      (transition-phase :play face)))

(register-handler
 :set-active-card
 [(undoable)
  standard-middleware]
 set-active-card-handler)

(defn set-target-handler [db [target-id]]
  (let [active-card (:active-card db)]
    (-> db
        (assoc-in [:card-target] target-id)
        (assoc :guard-guess nil)
        (transition-phase :target active-card))))

(register-handler
 :set-target
 [(undoable) standard-middleware]
 set-target-handler)

(defn set-guard-guess-handler [db [face]]
  (-> db
      (assoc-in [:guard-guess] face)
      (transition-phase :guard nil)))

(register-handler
 :set-guard-guess
 [(undoable) standard-middleware]
 set-guard-guess-handler)

(defn retrieve-card [db face current-player]
  (->> (get-in db [:game/players current-player :player/hand])
       (filter #(= face (:card/face %)))
       first))

(defn play-card [db face player-id]
  (let [path           [:game/players player-id :player/hand]
        discarded-card (retrieve-card db face player-id)]
    (if (nil? discarded-card)
      (throw (js/Error. "Tried to discard a nil card"))
      (-> db
          (assoc-in path (u/remove-first (get-in db path) face))
          (update-in [:game/discard-pile] conj discarded-card)))))

(defn handle-draw-card [db player-id]
  (-> (g/move-card db [:game/deck] [:game/players player-id :player/hand])
      (set-phase :play)))

(register-handler
 :draw-card
 [(undoable) standard-middleware]
 (fn [db [player-id]]
   (handle-draw-card db player-id)))

(defn action->message [player-id active-card target guard-guess]
  (str "Player " player-id
       (case active-card
         :guard    (str " guesses Player " target " has a " (s/capitalize (name guard-guess)))
         :priest   (str " uses the Priest to peek at Player " target "'s hand")
         :baron    (str " uses the Baron to compare cards with Player " target)
         :handmaid (str " uses the Handmaid to protect themself")
         :prince   (str " uses the Prince to force Player " target " to discard their card")
         :king     (str " uses the King to trade hands with Player " target)
         :countess (str " discards the Countess")
         :princess (str " loses by discarding the Princess")
         :default "error")))

(defn resolve-effect [{:keys [card-target active-card guard-guess current-player] :as db}]
  (case active-card
    :guard    (g/guard-ability         db guard-guess card-target)
    :priest   (g/reveal-card-to-player db current-player card-target)
    :baron    (g/baron-ability         db current-player card-target)
    :handmaid (g/handmaid-ability      db current-player)
    :prince   (g/prince-ability        db card-target)
    :king     (g/king-ability          db current-player card-target)
    :countess db
    :princess (g/kill-player db current-player)
    :default  db))

(defn append-to-log [{:keys [active-card
                             current-player
                             guard-guess
                             card-target] :as game}]
  (update game :log conj
          (action->message
           current-player
           active-card
           card-target
           guard-guess)))

(defn no-op-message [game active-card]
  (update game :log conj (str "Player " (:game/current-player game) " plays the "
                              (s/capitalize (name active-card)) " with no effect")))

(defn simulate-turn [game]
  (let [{:keys [current-player]} game
        with-card-drawn (handle-draw-card game current-player)
        action (->> (ai/generate-actions with-card-drawn current-player)
                    ai/pick-action
                    :action)]
    (if (seq (u/valid-targets with-card-drawn))
      (-> with-card-drawn
          (merge action)
          (play-card (:active-card action) current-player)
          append-to-log
          resolve-effect
          start-next-turn)
      (-> with-card-drawn
          (merge action)
          (play-card (:active-card action) current-player)
          (no-op-message (:active-card action))
          start-next-turn))))

(register-handler
 :simulate-turn
 [(undoable) standard-middleware]
 simulate-turn)

(register-handler
 :resolve-effect
 [(undoable) standard-middleware]
 (fn [{:keys [active-card current-player] :as db} _]
   (-> db
       (play-card active-card current-player)
       append-to-log
       resolve-effect
       start-next-turn)))

(register-handler
 :discard-without-effect
 [(undoable) standard-middleware]
 (fn [{:keys [active-card current-player] :as db}]
   (-> db
       (play-card active-card current-player)
       (no-op-message active-card)
       start-next-turn)))

(register-handler
 :toggle-debug-mode
 standard-middleware
 (fn [db]
   (update db :debug-mode? not)))

(register-handler
 :load-game
 standard-middleware
 (fn [db [_ game]]
   (merge db game)))

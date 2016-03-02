(ns love-letter-cljs.subs
    (:require-macros [reagent.ratom :refer [reaction]])
    (:require [re-frame.core :refer [register-sub subscribe]]
              [love-letter-cljs.game :refer [valid-targets]]))

(register-sub
 :deck
 (fn [db]
   (reaction (get-in @db [:game :deck]))))

(register-sub
 :burn-pile
 (fn [db]
   (reaction (get-in @db [:game :burn-pile]))))

(register-sub
 :current-player
 (fn [db]
   (reaction (get-in @db [:game :current-player]))))

(register-sub
 :players
 (fn [db]
   (reaction
    (->> (get-in @db [:game :players])
         vals
         vec))))

(def card-info
  {:guard    {:face "Guard"    :value 1 :text "Name a non-Guard card and choose another player. If that player has that card he or she is out of the round."}
   :priest   {:face "Priest"   :value 2 :text "Look at another player's hand."}
   :baron    {:face "Baron"    :value 3 :text "You and another player secretly compare hands. The player with the lower value is out of the round."}
   :handmaid {:face "Handmaid" :value 4 :text "Until your next turn, ignore all effects from other player's card."}
   :prince   {:face "Prince"   :value 5 :text "Choose any player (including yourself) to discard his or her hand and draw a new card."}
   :king     {:face "King"     :value 6 :text "Trade hands with anotherp layer of your choice."}
   :countess {:face "Countess" :value 7 :text "If you have this card and the King or Prince in your hand, you must discard this card."}
   :princess {:face "Princess" :value 8 :text "If you disacrd this card, you are out of the round.."}})

(register-sub
 :display-card
 (fn [db]
   (reaction (get-in @db [:state :display-card]))))
(register-sub
 :db
 (fn [db]
   (reaction @db)))

(register-sub
 :current-player-info
 (fn [db]
   (let [current-player (subscribe [:current-player])]
     (reaction (get-in @db [:game :players @current-player])))))

(register-sub
 :current-phase
 (fn [db]
   (reaction (get-in @db [:state :phase]))))

(register-sub
 :valid-targets
 (fn [db]
   (reaction (valid-targets (:game @db)))))

(register-sub
 :app-state
 (fn [db]
   (reaction (:state @db))))

(register-sub
 :log
 (fn [db]
   (let [state (subscribe [:app-state])]
     (reaction (:log @state)))))

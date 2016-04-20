(ns love-letter-cljs.subs
    (:require-macros [reagent.ratom :refer [reaction]])
    (:require [re-frame.core :refer [register-sub subscribe]]
              [love-letter-cljs.utils :refer [valid-targets]]))

(register-sub
 :deck
 (fn [db]
   (reaction (@db :deck))))

(register-sub
 :burn-pile
 (fn [db]
   (reaction (@db :burn-pile))))

(register-sub
 :discard-pile
 (fn [db]
   (reaction (@db :discard-pile))))

(register-sub
 :current-player
 (fn [db]
   (reaction (@db :current-player))))

(register-sub
 :players
 (fn [db]
   (reaction
    (->> (@db :players)
         vals
         vec))))

(register-sub
 :display-card
 (fn [db]
   (reaction (@db :display-card))))

(register-sub
 :db
 (fn [db]
   (reaction @db)))

(register-sub
 :current-player-info
 (fn [db]
   (let [current-player (subscribe [:current-player])]
     (reaction (get-in @db [:players @current-player])))))

(register-sub
 :current-phase
 (fn [db]
   (reaction (@db :phase))))

(register-sub
 :valid-targets
 (fn [db]
   (reaction (valid-targets @db))))

(register-sub
 :debug-mode
 (fn [db]
   (reaction (@db :debug-mode?))))

(register-sub
 :log
 (fn [db]
   (reaction (:log @db))))

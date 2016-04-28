(ns love-letter-cljs.subs
    (:require-macros [reagent.ratom :refer [reaction]])
    (:require [cljs.core.match :refer-macros [match]]
              [re-frame.core :refer [register-sub subscribe]]
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
 :player-info
 (fn [db [_ player-id]]
   (reaction (get-in @db [:players player-id]))))

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

(register-sub
 :active-screen
 (fn [db]
   (reaction (:active-screen @db))))

(defn resolvable? [face card-target? guard-guess?]
  (match [face card-target? guard-guess?]
    [:guard  true true] true
    [:priest true _] true
    [:baron  true _] true
    [:handmaid _  _] true
    [:prince true _] true
    [:king   true _] true
    [:countess  _ _] true
    [:king   true _] true
    [:princess _  _] true
    :else false))

(def not-nil? (complement nil?))


(defn display-guard-guess? [face target]
  (and (= face :guard)
       (not= nil target)))

(defn display-target? [face]
  (case face
    :princess false
    :countess false
    :handmaid false
    nil       false
    true))

(register-sub
 :display-target?
 (fn [db]
   (reaction
    (let [{:keys [active-card]} @db]
      (display-target? active-card)))))


(register-sub
 :display-guard-guess?
 (fn [db]
   (reaction
    (let [{:keys [active-card card-target]} @db]
      (display-guard-guess? active-card card-target)))))

(register-sub
 :resolvable?
 (fn [db]
   (reaction
    (let [{:keys [active-card card-target guard-guess]} @db]
      (resolvable? active-card (not-nil? card-target) (not-nil? guard-guess))))))

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

(register-sub
 :score-game
 (fn [db]
   (reaction (score-game @db))))

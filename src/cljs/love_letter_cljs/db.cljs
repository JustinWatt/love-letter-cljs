(ns love-letter-cljs.db
  (:require [love-letter-cljs.game :refer [create-and-deal]]
            [schema.core :as s
             :include-macros true]))

(def card-face
  (s/enum :guard  :priest :baron    :handmaid
          :prince :king   :countess :princess))

(def player-id s/Int)

(def card {:face card-face
           :value s/Int
           :visible [player-id]})

(def card-pile [(s/maybe card)])

(def player {:id     s/Int
             :hand   card-pile
             :alive? s/Bool
             :protected? s/Bool})

(def app-schema
  {:active-screen (s/enum :title-screen :main-screen :debug-screen :game-screen)
   :deck         card-pile
   :discard-pile card-pile
   :burn-pile    card-pile
   :players      {s/Int player}
   :current-player player-id

   :display-card (s/maybe card-face)
   :phase        (s/enum :draw :play :guard :target :resolution :complete)
   :active-card  (s/maybe card-face)
   :guard-guess  (s/maybe card-face)
   :card-target  (s/maybe player-id)
   :debug-mode?   s/Bool
   :log          (s/maybe [(s/maybe {:from s/Str :time s/Str :message s/Str})])})

(def default-db
  (merge
   (create-and-deal)
   {:active-screen :title-screen
    :display-card nil
    :phase :draw
    :active-card nil
    :guard-guess nil
    :card-target nil
    :debug-mode? true
    :log [{:from "System"
           :time (.toLocaleTimeString (js/Date.))
           :message "Welcome to the Game"}]}))

(def action-types
  {:guard    [:high-card :eliminate :survive]
   :priest   [:high-card :assist]
   :baron    [:high-card :eliminate :survive]
   :handmaid [:high-card :defense]
   :prince   [:high-card :eliminate]
   :king     [:high-card :assist]
   :countess [:high-card :assist]
   :princess [:high-card :suicide]})

(def action
  {:card          :guard
   :value         1
   :action-type   :eliminate
   :target-player 2
   :target-card   :baron
   :action-score  30})

(defn valid-schema?
  "validate the given db, writing any problems to console.error"
  [db]
  (let [res (s/check app-schema db)]
    (if (some? res)
      (.error js/console (str "schema problem: " res)))))

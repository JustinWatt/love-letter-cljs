(ns love-letter-cljs.db
  (:require [love-letter-cljs.game :refer [create-and-deal]]
            [schema.core :as s
             :include-macros true]))

(def card-face
  (s/enum :guard  :priest :baron    :handmaid
          :prince :king   :countess :princess))

(def card {:face card-face
           :value s/Int})

(def card-pile [(s/maybe card)])

(def player {:id     s/Int
             :hand   card-pile
             :alive? s/Bool
             :protected? s/Bool})

(def player-id s/Int)

(def app-schema
  {:game {:deck         card-pile
          :discard-pile card-pile
          :burn-pile    card-pile
          :players      {s/Int player}
          :current-player player-id
          :selected-card (s/maybe card-face)}

   :state {:display-card (s/maybe card-face)
           :phase        (s/enum :draw :play :guard :target :resolution)
           :active-card  (s/maybe card-face)
           :guard-guess  (s/maybe card-face)
           :card-target  (s/maybe player-id)
           :log          [(s/maybe {:time s/Str :message s/Str})]}})

(def default-db {:game  (create-and-deal)

                 :state {:display-card nil
                         :phase :draw
                         :active-card nil
                         :guard-guess nil
                         :card-target nil
                         :log [{:from "System" :date (.toLocaleTimeString (js/Date.)) :message "Welcome to the Game"}]}})

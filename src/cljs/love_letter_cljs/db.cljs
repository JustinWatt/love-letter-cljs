(ns love-letter-cljs.db
  (:require [love-letter-cljs.game :refer [create-and-deal]]
            [schema.core :as s :include-macros true]))

(def card-face
  (s/enum :guard  :priest :baron    :handmaid
          :prince :king   :countess :princess))

(def ai-profile
  (s/enum :aggressive :defensive :base))

(def player-id s/Int)

(def card {:card/face card-face
           :card/value s/Int
           :card/visible [player-id]})

(def card-pile [(s/maybe card)])

(def player {:player/id     s/Int
             :player/hand   card-pile
             :player/alive? s/Bool
             :player/protected? s/Bool
             :player/personality ai-profile})

(def app-schema
  {:active-screen (s/enum :title-screen :main-screen :debug-screen :game-screen :win-screen)
   :game/deck         card-pile
   :game/discard-pile card-pile
   :game/burn-pile    card-pile
   :game/players      {s/Int player}
   :game/current-player player-id

   :display-card (s/maybe card-face)
   :phase        (s/enum :draw :play :guard :target :resolution :complete)
   :active-card  (s/maybe card-face)
   :guard-guess  (s/maybe card-face)
   :card-target  (s/maybe player-id)
   :debug-mode?   s/Bool
   :log          (s/maybe [s/Str])})

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
    :log []}))

(defn valid-schema?
  "validate the given db, writing any problems to console.error"
  [db]
  (let [res (s/check app-schema db)]
    (when (some? res)
      (.error js/console (str "schema problem: " res)))))

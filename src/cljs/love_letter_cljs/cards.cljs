(ns love-letter-cljs.cards
  (:require [reagent.core :as reagent]
            [devcards.core])
  (:require-macros [devcards.core :as dc
                    :refer [defcard defcard-rg]]))

(defn deck [card-count]
  [:div {:style {:height "120px"
                 :width "60px"
                 :font-family "Press Start 2P"
                 :color "white"
                 :background-color "black"}}
   [:div {:style {:height "90px"
                  :width  "60px"
                  :border-width "3px"
                  :border-style "solid"
                  :background-size "contain"
                  :background-image "url(images/cardart/card-back-pixelated.png)"
                  :border-radius "5"
                  :border-color "blue"}}]
   [:h4.text-center (str card-count)]])


(defcard-rg render-deck
  [deck :guard 11])

(defn discard-pile [cards]
  [:div
   [:div {:style {:position "absolute"
                  :height "120px"
                  :width  "450px"
                  :background-color "black"}}
    (map-indexed (fn [i c]
                   [:div {:style {:position "absolute"
                                  :left (str (* 30 i) "px")
                                  :width "60px"
                                  :height "90px"
                                  :border-color "black"
                                  :border-style "solid"
                                  :background-size "contain"
                                  :background-image "url(images/cardart/card-back-pixelated.png)"
                                  :border-width "2px"}}]) cards)]])


(defcard-rg render-discard
  [discard-pile [1 2 3 4 5 7 8 9 10 11 12 13 14 15]])



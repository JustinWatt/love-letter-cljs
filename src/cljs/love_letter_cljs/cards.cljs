(ns love-letter-cljs.cards
  (:require [reagent.core :as reagent]
            [devcards.core])
  (:require-macros [devcards.core :as dc
                    :refer [defcard defcard-rg]]))

(defn deck [phase card-count]
  [:div {:style {:height "120px"
                 :width "60px"
                 :background-color "black"}}
   [:div {:style {:height "90px"
                  :width  "60px"
                  :border-width "3px"
                  :border-style "solid"
                  :border-radius "5"
                  :border-color "blue"}}]
   [:h4.text-center (str card-count)]])

(defcard-rg render-deck
  [deck :guard 11])



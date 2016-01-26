(ns love-letter-cljs.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [clojure.string :as s]))

(defn card-item [card]
  (let [{:keys [face value]} card]
     ^{:key (rand-int 10000)} [:li (str (s/capitalize (name face)) " " value)]))

(defn card-list [deck]
  [:ul
  (for [card deck]
    ^{:key (rand-int 1000)}[card-item card])])

(defn player-component [player]
  (let [{:keys [id hand alive?]} player]
    [:div
     [:h5 (str "Player: " id)]
     [card-list hand]]))

(defn command-panel []
  [:div
   [:button {:on-click #(dispatch [:deal-cards])} "deal"]
   [:button {:on-click #(dispatch [:new-game])} "new game"]])

(defn main-panel []
  (let [deck    (subscribe [:deck])
        players (subscribe [:players])]
    (fn []
      [:div
       [:h3 "Deck:"]
       [card-list @deck]

       [:h3 "Players: "]
       (for [player @players]
         ^{:key (rand-int 1000)}[player-component player])
       [command-panel]])))

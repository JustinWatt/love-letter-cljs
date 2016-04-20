(ns love-letter-cljs.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [clojure.string :as s]))

(defn card-item [card]
  (let [{:keys [face value visible]} card]
    ^{:key (rand-int 10000)}
    [:li
     [:span
      {:on-mouse-over #(dispatch [:set-display-card face])
       :on-mouse-out  #(dispatch [:set-display-card nil])}
      (str (s/capitalize (name face)) " " value (when-not (empty? visible) (str " " visible)) )]]))

(defn card-list [deck]
  [:ul
   (map-indexed (fn [i card]
     ^{:key i} [card-item card]) deck)])

(defn play-button [face]
  [:button {:type "button"
            :on-click #(dispatch [:set-active-card face])} (name face)])

(defn card-list-with-button [deck phase]
  [:ul
   (when (= phase :play)
     (map-indexed
      (fn [i card]
        ^{:key (str "card" i)}
        [:li [play-button (:face card)]]) deck))])

(defn draw-button [id]
  [:button {:type "button"
            :on-click #(do (dispatch [:draw-card id])
                           (dispatch [:set-phase :play]))} "Draw"])

(defn player-component [player]
  (let [{:keys [id hand alive? protected?]} player]
    [:div.col-md-6
     [:h5 (str "Player " id (when-not alive? " Out") (when protected? " Protected") ":")]
     [card-list hand]]))

(defn command-panel []
  [:div
   [:button {:on-click #(dispatch [:new-game])} "new game"]
   [:button {:on-click #(dispatch [:simulate-turn])} "simulate turn"]])

(defn card-display []
  (let [card (subscribe [:display-card])]
    (fn []
      (when @card
        (let [img-src (str "images/cardart/" (name @card) ".png")]
          [:img.img-responsive {:src  img-src}])))))

(defn target-control []
  (let [targets (subscribe [:valid-targets])]
    (fn []
      [:div
       (if (empty? @targets)
         [:button
          {:on-click #(dispatch [:discard-without-effect])}
          "No Valid Targets"]
         [:ul
          (map-indexed
           (fn [i target]
             ^{:key (str "target" i)}
             [:button
              {:on-click #(dispatch [:set-target target])}
              (str "Player " target)])
           @targets)])])))

(def card-faces
  [:priest
   :baron
   :handmaid
   :prince
   :king
   :countess
   :princess])

(defn guard-guess-button [face]
  [:button {:on-click #(dispatch [:set-guard-guess face])} (name face)])

(defn guard-control []
  [:div
   (map-indexed (fn [i face]
                  ^{:key (str "guess" i)}
                     [guard-guess-button face]) card-faces)])

(defn game-controls []
  (let [player-info  (subscribe [:current-player-info])
        phase        (subscribe [:current-phase])]
    (fn []
      [:div [:h4 (str "Current phase"@phase)]
       (condp = @phase
         :draw [:div [card-list (:hand @player-info)] [draw-button (:id @player-info)]]
         :play [card-list-with-button (:hand @player-info) @phase]
         :target [target-control]
         :guard  [guard-control]
         :resolution [:button {:on-click #(dispatch [:resolve-effect])} "Resolve"]
         :complete [:div "Woo"]
         [:div "error"])])))

(defn log-panel []
  (let [log (subscribe [:log])]
    (fn []
      [:div
       [:h3 "Game Log:"]
       [:ul
        (map-indexed (fn [id {date :date message :message from :from}]
                       ^{:key id}[:li (str date "| " from "> " message)]) (take 10 @log))]])))

(defn secret-controls []
  (let [redos? (subscribe [:redos?])
        undos? (subscribe [:undos?])
        undo-list (subscribe [:undo-explanations])
        redo-list (subscribe [:redo-explanations])]
    [:div
     [:button {:on-click #(dispatch [:undo]) :disabled (not @undos?)}(str "Undo " (count @undo-list))]
     [:button {:on-click #(dispatch [:redo]) :disabled (not @redos?)}(str "Redo " (count @redo-list) )]
     [:button {:on-click #(dispatch [:purge-redos]) :disabled (not @redos?)} (str "Clear History")]
     ]))


(defn main-panel []
  (let [deck           (subscribe [:deck])
        players        (subscribe [:players])
        burn-pile      (subscribe [:burn-pile])
        discard-pile   (subscribe [:discard-pile])
        current-player (subscribe [:current-player])
        db             (subscribe [:db])
        debug?         (subscribe [:debug-mode])]
    (fn []
      [:div.container-fluid
       [:button {:on-click #(dispatch [:toggle-debug-mode])} "Debug Mode"]
       (if-not @debug?
         [:div "Debug Mode"]

         [:div
          [:div.row
           [:div.col-md-4.col-sm-4.col-xs-4
            [:h3 "Deck ("(str (count @deck))") :"]
            [card-list @deck]]

           [:div.col-md-4.col-sm-4.col-xs-4
            [:h3 "Players: "]
            (map-indexed
             (fn [i player]
               ^{:key (str "player-" i)} [player-component player])
             @players)]

           [:div.col-md-4.col-sm-4.col-xs-4
            [secret-controls]
            [:h3 "Burn Pile ("(str (count @burn-pile))")"]
            [card-list @burn-pile]
            [command-panel]
            [:h6 (str @db)]]]

          [:div.row
           [:div.col-md-4.col-sm-4.col-xs-4
            [:h1 (str "Current Player: " @current-player)]
            [game-controls]]

           [:div.col-md-4.col-sm-4.col-xs-4
            [:h3 "Discard Pile ("(str (count @discard-pile))")"]
            #_[:div {:style {:height "50px" :width "35px"
                           :background-color "#7f0000"
                           :border-radius "3px"
                           :text-align "center"
                           :vertical-align "50%"
                           :color "white"}} (str (count @deck))]
            [card-list @discard-pile]]

           [:div.col-md-4.col-sm-4.col-xs-4
            [card-display]]


           [:div.col-md-6.col-sm-6.col-xs-6
            #_[log-panel]]]])])))


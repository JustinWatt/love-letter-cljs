(ns love-letter-cljs.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [clojure.string :as s]))

(defn card-item [card]
  (let [{:keys [face value]} card]
    ^{:key (rand-int 10000)}
    [:li
     [:span
      {:on-mouse-over #(dispatch [:set-display-card face])
       :on-mouse-out  #(dispatch [:set-display-card nil])}
      (str (s/capitalize (name face)) " " value)]]))

(defn card-list [deck]
  [:ul
   (map-indexed (fn [i card]
     ^{:key i} [card-item card]) deck)])

(defn play-button [face]
  [:button {:type "button"
            :on-click #(dispatch [:set-active-card face])} (name face)])

(defn card-list-with-button [deck phase]
  [:ul
   (for [card deck]
     (do [card-item card]
         (when (= phase :play) ^{:key (rand-int 1000)}
           [:li
            [play-button (:face card)]])))])

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
   [:button {:on-click #(dispatch [:new-game])} "new game"]])

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
       [:ul
        (for [t @targets]
          ^{:key (str "target-" t)}
          [:button {:id (str "target-" t)
                    :on-click #(dispatch [:set-target t])} (str "Player " t)])]])))

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

(defn main-panel []
  (let [deck           (subscribe [:deck])
        players        (subscribe [:players])
        burn-pile      (subscribe [:burn-pile])
        discard-pile      (subscribe [:discard-pile])
        current-player (subscribe [:current-player])
        app-state      (subscribe [:app-state])
        db             (subscribe [:db])]
    (fn []
      [:div.container-fluid
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
         [:h3 "Burn Pile ("(str (count @burn-pile))")"]
         [card-list @burn-pile]
         [command-panel]
         [:div (str @app-state)]]]

       [:div.row
        [:div.col-md-4.col-sm-4.col-xs-4
         [:h1 (str "Current Player: " @current-player)]
         [game-controls]]

        [:div.col-md-4.col-sm-4.col-xs-4
         [:h3 "Discard Pile ("(str (count @discard-pile))")"]
         [card-list @discard-pile]]

        [:div.col-md-4.col-sm-4.col-xs-4
          [card-display]]


        [:div.col-md-6.col-sm-6.col-xs-6
         #_[log-panel]]]])))

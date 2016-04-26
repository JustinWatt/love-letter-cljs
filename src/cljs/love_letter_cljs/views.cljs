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
            :on-click #(dispatch [:draw-card id])} "Draw"])

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
     [:button {:on-click #(dispatch [:purge-redos]) :disabled (not @redos?)} (str "Clear History")]]))

(defn draw-deck [card-count]
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


(defn game-container [children]
  [:div.title-screen.flex-container {:style {:margin "auto"
                                             :width 904
                                             :height 480
                                             :background-color "black"}}
   children])

(defn title-screen []
  [:div.title-screen.flex-container
   [:h1#title.text-center.flex-item "Love Letter"]
   [:h4#start-button.text-center {:on-click #(dispatch [:set-active-screen :game-screen])} "Start"]])

(defn main-panel []
  (let [deck           (subscribe [:deck])
        players        (subscribe [:players])
        burn-pile      (subscribe [:burn-pile])
        discard-pile   (subscribe [:discard-pile])
        current-player (subscribe [:current-player])
        db             (subscribe [:db])]
    (fn []
      [:div.container-fluid {:style {:color "grey"}}
       [:button {:on-click #(dispatch [:set-active-screen :game-screen])} "Exit Debug"]
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
            #_[:h6 (str @db)]]]

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
            [card-display]]]]])))

(defn- card-spread [cards on-click classes]
  (map-indexed
   (fn [i c]
     ^{:key (str i (:face c))}
     [:div.discarded-card
      {:style {:position "absolute"
               :left (str (* 30 i) "px")
               :width "65px"
               :height "85px"
               :border-radius "5px"
               :border-color "black"
               :border-style "solid"
               :background-size "contain"
               :background-image (str "url(images/cardart/" (name (:face c)) ".png)")
               :border-width "2px"}}]) cards))

(defn discard-pile [cards]
  [:div
   [:div {:style {:position "relative"
                  :height "120px"
                  :width  "425px"
                  :background-color "darkgray"}}
    (map-indexed (fn [i c]
                   ^{:key (str i (:face c))}
                   [:div.discarded-card
                    {:style {:position "absolute"
                             :left (str (* 30 i) "px")
                             :width "65px"
                             :height "85px"
                             :border-radius "5px"
                             :border-color "black"
                             :border-style "solid"
                             :background-size "contain"
                             :background-image (str "url(images/cardart/" (name (:face c)) ".png)")
                             :border-width "2px"}}]) cards)]])

(defn player-one-area [player-info phase active-player?]
  [:div (str player-info)
   [:div (card-spread
          (:hand player-info)
          nil nil
          )]])

(defn game-screen []
  (let [deck              (subscribe [:deck])
        discarded-cards   (subscribe [:discard-pile])
        current-player-id (subscribe [:current-player])
        phase             (subscribe [:current-phase])
        player-one-info   (subscribe [:player-info 1])]
    (fn []
      [:div {:style {:font-family "'Press Start 2P', cursive"
                     :color "white"}}
       [:button {:on-click #(dispatch [:set-active-screen :debug-screen])} "Debug"]
       [:button {:on-click #(dispatch [:simulate-turn])} "Simulate Turn"]
       [:button {:on-click #(dispatch [:new-game])} "New Game"]

       [:h3.text-center (str "Player " @current-player-id ", go!")]
       [:h3.text-center (str (s/capitalize (name @phase)))]

       [:div#draw-pile {:on-click (when (= @phase :draw ) #(dispatch [:draw-card @current-player-id]))
                        :style {:position "absolute" :left "27%" :bottom "40%"}}
        [draw-deck (str (count @deck))]]
       [:div#discard-pile {:style {:position "absolute" :left "35%" :bottom "39%"}}
        [discard-pile @discarded-cards]]
       [:div#player-one {:style {:position "absolute" :left "25%" :bottom "5%"}}
        [player-one-area @player-one-info]]


       ])))

(defmulti screens identity)
(defmethod screens :title-screen [] [title-screen])
(defmethod screens :debug-screen [] [main-panel])
(defmethod screens :game-screen [] [game-screen])
(defmethod screens :default [] [:div])

(defn main-screen []
  (let [active-screen (subscribe [:active-screen])]
    (fn []
      [game-container 
       (screens @active-screen)])))


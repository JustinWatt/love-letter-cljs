(ns love-letter-cljs.ai)


(def cards [{:face :guard    :value 1 :count 5}
            {:face :priest   :value 2 :count 2}
            {:face :baron    :value 3 :count 2}
            {:face :handmaid :value 4 :count 2}
            {:face :prince   :value 5 :count 2}
            {:face :king     :value 6 :count 1}
            {:face :countess :value 7 :count 1}
            {:face :princess :value 8 :count 1}])

(defn generate-card [card]
  (let [{:keys [count face value]} card]
    (repeat count {:face face :value value})))

(defn generate-deck []
  (vec (shuffle (mapcat generate-card cards))))

;;; AI STUFF

;;;

(defn remove-first [face coll]
  (let [[pre post] (split-with #(not= face (:face %)) coll)]
    (vec (concat pre (rest post)))))


(def test-game
  {:deck [{:face :priest   :value 2}
          {:face :baron    :value 3}
          {:face :prince   :value 5}
          {:face :countess :value 7}
          {:face :guard    :value 1}]

   :discard-pile [{:face :guard :value 1}
                  {:face :guard :value 1}
                  {:face :prince :value 5}
                  {:face :priest :value 2}
                  {:face :handmaid :value 4}]

   :burn-pile [{:face :handmaid :value 4}]

   :players {1 {:id 1
                :hand [{:face :king  :value 6}
                       {:face :baron :value 3}]
                :alive? true
                :protected? false}

             2 {:id 2
                :hand [{:face :guard :value 1}]
                :alive? true
                :protected? false}

             3 {:id 3
                :hand [{:face :princess :value 8}]

                :alive? true

                :protected? false}
             4 {:id 4

                :hand [{:face :guard :value 1}]

                :alive? true :protected? true}}

   :current-player 1})

(defn player-hand [game id]
  (get-in game [:players id :hand]))


(defn filter-fresh-deck [card-list]
  (reduce (fn [deck card]
           (remove-first card deck)) (generate-deck) card-list))

(defn high-card [game card]
  (let [{:keys [discard-pile current-player]} game
        {:keys [face value]} card

        current-player-hand (player-hand game current-player)

        known-list (vec (map :face (concat discard-pile current-player-hand)))

        filtered-deck (filter-fresh-deck known-list)]

    (float (/ (count (filter #(>= value (:value %)) filtered-deck))
              (count filtered-deck)))))

(high-card test-game {:face :king :value 6})

[{:face :guard, :value 1}
 {:face :baron, :value 3}
 {:face :guard, :value 1}
 {:face :priest, :value 2}
 {:face :handmaid, :value 4}
 {:face :princess, :value 8}
 {:face :prince, :value 5}
 {:face :guard, :value 1}
 {:face :countess, :value 7}]

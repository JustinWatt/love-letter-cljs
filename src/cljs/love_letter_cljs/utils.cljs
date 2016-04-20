(ns love-letter-cljs.utils)

(defn find-card [game target]
  (-> game
      (get-in [:players target :hand])
      peek))

(defn- valid-target? [current-player player]
  (and (not= current-player (:id player))
       (and (not (:protected? player))
            (:alive? player))))

(defn valid-targets [game]
  (let [current-player (:current-player game)]
    (-> game
        :players
        vals
        (->>
         (filter (partial valid-target? current-player))
         (map :id)))))

(defn remove-first [face coll]
  (let [[pre post] (split-with #(not= face (:face %)) coll)]
    (vec (concat pre (rest post)))))


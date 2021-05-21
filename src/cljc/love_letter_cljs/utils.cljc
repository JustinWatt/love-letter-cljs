(ns love-letter-cljs.utils)

(defn find-card [game target]
  (-> game
      (get-in [:game/players target :player/hand])
      first))

(defn- valid-target?
  [current-player
   {:player/keys [id protected? alive?]}]
  (and (not= current-player id)
       (and alive? (not protected?))))

(defn valid-targets [{:keys [current-player players]}]
  (->> players
       vals
       (filter (partial valid-target? current-player))
       (map :player/id)))

(defn remove-first [coll face]
  (let [[pre post] (split-with #(not= face (:card/face %)) coll)]
    (vec (concat pre (rest post)))))

;; For cycling turns
(defn next-in-list [item-list current]
  (as-> item-list i
    (filter #(> % current) i)
    (or (first i)
        (first item-list))))

(defn player-list [game]
  (->> game
       :game/players
       vals
       (filter :player/alive?)
       (mapv :player/id)))

(defn set-phase [game phase]
  (assoc-in game [:phase] phase))
